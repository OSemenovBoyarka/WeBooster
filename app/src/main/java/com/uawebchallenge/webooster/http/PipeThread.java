package com.uawebchallenge.webooster.http;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Passes traffic from input to output stream
 */
class PipeThread extends Thread {

    private static final String TAG = "PipeThread";

    private InputStream from;
    private OutputStream to;

    public PipeThread(InputStream from, OutputStream to) {
        this.from = from;
        this.to = to;
    }
    @Override
    public void run() {
        byte[] buffer = new byte[2048];
        int readCount;
        try {
            while((readCount=from.read(buffer))!=-1) {
                to.write(buffer, 0, readCount);
                to.flush();
            }
            from.close();
        } catch (IOException e) {
            Log.w(TAG, "Failed to transfer data between streams: "+e.getMessage());
        }
    }
}
