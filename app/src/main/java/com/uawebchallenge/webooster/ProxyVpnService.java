package com.uawebchallenge.webooster;

import com.runjva.sourceforge.jsocks.protocol.PortForwardRule;
import com.runjva.sourceforge.jsocks.protocol.ProxyServer;
import com.runjva.sourceforge.jsocks.server.ServerAuthenticatorNone;
import com.uawebchallenge.webooster.http.HttpCompressingProxyServer;

import org.torproject.android.vpn.Tun2Socks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;


/**
 * @author Alexander Semenov
 */
public class ProxyVpnService extends VpnService implements Runnable {

    private static final String TAG = ProxyVpnService.class.getSimpleName();

    public static final String DISCONNECT_ACTION = ProxyVpnService.class.getName()
            + ".DISCONNECT_VPN";

    final private BroadcastReceiver stopServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopLocalVpnProxy();
            stopSelf();
        }
    };

    private Thread vpnThread;

    //Socks server, used to forward all traffic from tun2socks further to network
    // connections on port 80 are captured and forwarded to HttpCompressingProxyServer, all other connection are forwarded directly
    private ProxyServer vpnForwardProxyServer;

    //this is probably not the best solution, by I didn't had time to make something better
    //this another socks proxy is needed to let http connections get to web from HttpCompressingProxyServer, bypassing VPN,
    //it seems to be not possible to protect individual sockets, created from HttpUrlConnection or OkHttp framework
    //TODO use simple HTTP only proxy instead of this
    private ProxyServer outgoingProxyServer;

    private HttpCompressingProxyServer httpProxyServer;

    private static final int VPN_MTU = 1500;

    private static final int SOCKS_PROXY_PORT = 7996;
    private static final int SOCKS_SOCKS_OUT_PROXY_PORT = 7998;

    public static final int HTTP_PROXY_PORT = 8080;

    private final int SOCKS_PROXY_MAX_PARALLEL_CONNECTIONS = 15;

    private ParcelFileDescriptor vpnInterface;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Stop the previous session by interrupting the thread.
        stopLocalVpnProxy();
        vpnThread = new Thread(this, "ProxyVpnThread");
        vpnThread.start();
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerReceiver(stopServiceReceiver, new IntentFilter(DISCONNECT_ACTION));
    }

    @Override
    public void onDestroy() {
        stopLocalVpnProxy();
        unregisterReceiver(stopServiceReceiver);
    }

    public void stopLocalVpnProxy() {
        if (vpnThread != null) {
            vpnThread.interrupt();
        }
        if (vpnInterface != null) {
            try {
                Log.d(TAG, "closing interface, destroying VPN interface");

                vpnInterface.close();
                vpnInterface = null;

            } catch (Exception | Error e) {
                Log.d(TAG, "error stopping tun2socks", e);
            }
            Tun2Socks.Stop();
        }
        stopSocksBypass();
        stopHttpServer();
    }


    @Override
    public void run() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            startSocksBypass(localHost);
            startHttpProxy(localHost);

            final String virtualGateway = "10.10.10.1";
            final String virtualIP = "10.10.10.2";
            final String virtualNetMask = "255.255.255.0";
            final String defaultRoute = "0.0.0.0";

            final String localSocks = "127.0.0.1" + ":" + SOCKS_PROXY_PORT;

            //TODO replace this with actual DNS forwarder like badvpn-udpgw, see https://github.com/ambrop72/badvpn/blob/master/tun2socks/badvpn-tun2socks.8
            //TODO alternatively - use system provided DNS
            String udpgwDNSServerAddress = "8.8.8.8:53";

            Builder builder = new Builder();
            builder.setSession(getString(R.string.app_name));
            builder.setMtu(VPN_MTU);
            builder.addAddress(virtualGateway, 32);

            //we will route all traffic
            builder.addRoute(defaultRoute, 0);

            vpnInterface = builder.establish();
            if (vpnInterface == null) {
                Log.e(TAG, "vpn interface is null");
                return;
            }

            Tun2Socks.Start(vpnInterface, VPN_MTU, virtualIP, virtualNetMask, localSocks,
                    udpgwDNSServerAddress, true);

        } catch (Exception e) {
            Log.i(TAG, "tun2Socks has stopped: " + e);
        }
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            vpnInterface = null;

        }
        stopSocksBypass();
        stopHttpServer();
    }


    private synchronized void startSocksBypass(final InetAddress localHost) {
        //jSOCKS works in blocking mode, so using own thread for it
                   final ServerAuthenticatorNone auth = new ServerAuthenticatorNone(null, null);
                    ProxyServer.setVpnService(ProxyVpnService.this);
        stopSocksBypass();
        //this server will handle http requests from our local proxy and forward them to interned with protected sockets
        new Thread() {

            public void run() {
                try {
                    outgoingProxyServer = new ProxyServer(auth);
                    outgoingProxyServer
                            .start(SOCKS_SOCKS_OUT_PROXY_PORT, SOCKS_PROXY_MAX_PARALLEL_CONNECTIONS,
                                    localHost);
                } catch (Exception e) {
                    Log.i(TAG, "Local outgoing proxy stopped: " + e.getMessage());
                    //correctly handle errors
                    stopLocalVpnProxy();
                }
            }
        }.start();

        new Thread() {

            public void run() {
                try {
                    vpnForwardProxyServer = new ProxyServer(auth);
                    //this will redirect all http traffic to our local proxy, which will compress it
                    vpnForwardProxyServer
                            .setPortForwardingRule(
                                    new PortForwardRule(80, HTTP_PROXY_PORT, localHost));
                    vpnForwardProxyServer
                            .start(SOCKS_PROXY_PORT, SOCKS_PROXY_MAX_PARALLEL_CONNECTIONS,
                                    localHost);
                } catch (Exception e) {
                    Log.i(TAG, "Local vpn bypass proxy stopped: " + e.getMessage());
                    //correctly handle errors
                    stopLocalVpnProxy();
                }
            }
        }.start();

    }

    private synchronized void stopSocksBypass() {
        if (vpnForwardProxyServer != null) {
            vpnForwardProxyServer.stop();
            vpnForwardProxyServer = null;
        }

        if (outgoingProxyServer != null) {
            outgoingProxyServer.stop();
            outgoingProxyServer = null;
        }
    }

    private void startHttpProxy(final InetAddress outProxyAddress) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                stopHttpServer();
                httpProxyServer = new HttpCompressingProxyServer();

                //let out conections pass
                SocketAddress outProxyAddr = new InetSocketAddress(outProxyAddress, SOCKS_SOCKS_OUT_PROXY_PORT);
                Proxy outProxy = new Proxy(Proxy.Type.SOCKS, outProxyAddr);

                httpProxyServer
                        .setVpnService(ProxyVpnService.this)
                        .setChainingProxy(outProxy);
                try {
                    httpProxyServer.start(HTTP_PROXY_PORT);
                } catch (IOException e) {
                    Log.i(TAG, "Local http proxy stopped: "+e.getMessage());
                    stopLocalVpnProxy();
                }
            }
        }).start();
    }

    private void stopHttpServer() {
        if (httpProxyServer != null) {
            httpProxyServer.stop();
            httpProxyServer = null;
        }
    }

}
