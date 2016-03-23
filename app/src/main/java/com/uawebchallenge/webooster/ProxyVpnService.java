package com.uawebchallenge.webooster;

import org.torproject.android.vpn.Tun2Socks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

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

    //xSocks
    private static final String VPN_ADDRESS = "26.26.26.1";

    private static final int VPN_MTU = 1500;

    private static final int PROXY_PORT = 8080;

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


    }


    @Override
    public void run() {

        //magic goes here :)
//
//        Thread forwarderThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (!Thread.currentThread().isInterrupted()) {
//                    try {
//                        Log.d(TAG, "run: listening for network requests...");
//                        ServerSocket serverSocket = new ServerSocket(PROXY_PORT);
//                        Socket socket = serverSocket.accept();
//                        Log.d(TAG, "run: accepted socket: " + socket);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//            }
//        });
//        forwarderThread.start();
        try {
            final String virtualGateway = "10.10.10.1";
            final String virtualIP = "10.10.10.2";
            final String virtualNetMask = "255.255.255.0";
            final String dummyDNS
                    = "8.8.8.8"; //this is intercepted by the tun2socks library, but we must put in a valid DNS to start
            final String defaultRoute = "0.0.0.0";

            final String localSocks = "127.0.0.1" + ":" + PROXY_PORT;
            final String localDns = "127.0.0.1" + ":" + PROXY_PORT;

            Builder builder = new Builder();
            builder.setSession(getString(R.string.app_name));
            builder.setMtu(VPN_MTU);
            builder.addAddress(virtualGateway, 32);
            builder.addDnsServer(dummyDNS);

            //we will route all traffic
            builder.addRoute(defaultRoute, 0);

            vpnInterface = builder.establish();
            if (vpnInterface == null) {
                Log.e(TAG, "vpn interface is null");
            }

            Tun2Socks.Start(vpnInterface, VPN_MTU, virtualIP, virtualNetMask, localSocks, localDns,
                    true);

        } catch (Exception e) {
            Log.d(TAG, "tun2Socks has stopped", e);
        }
        if (vpnInterface != null){
            try {
                vpnInterface.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            vpnInterface = null;
        }
    }

}
