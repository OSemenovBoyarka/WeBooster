package com.uawebchallenge.webooster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;

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
    }

    @Override
    public void run() {
        //magic goes here :)
        ParcelFileDescriptor pfd = new Builder()
                .addAddress("192.168.0.1", 32)
                .establish();
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(100);
                Log.d(TAG, "Listening connections");
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            pfd.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
