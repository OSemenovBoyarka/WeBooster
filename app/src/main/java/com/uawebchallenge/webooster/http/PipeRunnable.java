package com.uawebchallenge.webooster.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Passes traffic from input to output stream
 */
class PipeRunnable implements Runnable {
    private InputStream from;
    private OutputStream to;
    private Runnable onFinish;

    public PipeRunnable(InputStream from, OutputStream to) {
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
        } catch (IOException e) {}
        if (onFinish != null) onFinish.run();
    }
    public PipeRunnable setFinishAction(Runnable onClosed) {
        this.onFinish = onClosed;
        return this;
    }
}
