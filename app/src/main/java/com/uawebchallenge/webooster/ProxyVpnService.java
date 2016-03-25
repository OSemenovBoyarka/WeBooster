package com.uawebchallenge.webooster;

import com.runjva.sourceforge.jsocks.protocol.ProxyServer;
import com.runjva.sourceforge.jsocks.server.ServerAuthenticatorNone;

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
            stopProxy();
            stopSelf();
        }
    };

    private Thread vpnThread;

    /// tun2socks
    private Process tun2SocksProcess = null;

    //Socks server
    private ProxyServer mSocksProxyServer;

    private static final int VPN_MTU = 1500;

    private static final int SOCKS_PROXY_PORT = 7996;

    public static final int HTTP_PROXY_PORT = 7999;

    private final int SOCKS_PROXY_MAX_PARALLEL_CONNECTIONS = 50;

    private ParcelFileDescriptor vpnInterface;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Stop the previous session by interrupting the thread.
        stopProxy();
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
        stopProxy();
        unregisterReceiver(stopServiceReceiver);
    }

    public void stopProxy() {
        if (vpnThread != null) {
            vpnThread.interrupt();
        }
        if (vpnInterface != null) {
            try {
                Log.d(TAG, "closing interface, destroying VPN interface");

                vpnInterface.close();
                vpnInterface = null;

            } catch (Exception e) {
                Log.d(TAG, "error stopping tun2socks", e);
            } catch (Error e) {
                Log.d(TAG, "error stopping tun2socks", e);
            }
            Tun2Socks.Stop();
        }
        stopSocksBypass();


    }


    @Override
    public void run() {
        try {
            startSocksBypass();

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
                    udpgwDNSServerAddress,
                    false);

        } catch (Exception e) {
            Log.d(TAG, "tun2Socks has stopped", e);
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
    }


    private void startSocksBypass() {
        //jSOCKS works in blocking mode, so using own thread for it
        new Thread() {

            public void run() {
                if (mSocksProxyServer != null) {
                    stopSocksBypass();
                }
                try {
                    mSocksProxyServer = new ProxyServer(new ServerAuthenticatorNone(null, null));
                    ProxyServer.setVpnService(ProxyVpnService.this);
                    mSocksProxyServer.start(SOCKS_PROXY_PORT, SOCKS_PROXY_MAX_PARALLEL_CONNECTIONS,
                            InetAddress.getLocalHost());
                } catch (Exception e) {
                    Log.e(TAG, "error getting host", e);
                }
            }
        }.start();

    }

    private synchronized void stopSocksBypass() {

        if (mSocksProxyServer != null) {
            mSocksProxyServer.stop();
            mSocksProxyServer = null;
        }


    }

}
