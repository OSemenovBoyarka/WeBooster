package com.uawebchallenge.webooster.http;

import android.net.VpnService;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.net.SocketFactory;

/**
 * Proxy server for plain HTTP traffic, doesn't support HTTPS, will return BAD REQUEST for all
 * request
 *
 * Main traffic compression controller for the whole application
 *
 * @author Alexander Semenov
 */
@SuppressWarnings("EmptyCatchBlock")
public class HttpProxyServer {

    private static final String TAG = HttpProxyServer.class.getSimpleName();

    public static final String BAD_REUEST_RESPONSE = "HTTP/1.1 400 Bad Request\r\n";

    private boolean listening;

    private VpnService vpnService;

    private Set<Connection> connections = Collections.synchronizedSet(new HashSet<Connection>());

    private ServerSocket listenSocket;

    private SocketFactory socketFactory = SocketFactory.getDefault();

    public void setVpnService(VpnService vpnService) {
        this.vpnService = vpnService;
    }

    /**
     * Starts server on localhost.
     *
     * @param port port to listen connections
     * @throws IOException if error occurs while starting server
     */
    public void start(int port) throws IOException {
        if (listening) {
            throw new IOException("Server is running already");
        }
        listening = true;
        try {
            listenSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new IOException("Can't start local proxy on port " + port, e);
        }
        while (listening) {
            try {
                Connection connection = new Connection(listenSocket.accept());
                connection.start();
            } catch (IOException e) {
                Log.w(TAG, "Failed to proxy request", e);
            }
        }
    }

    public void stop() {
        listening = false;
        try {
            listenSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (Iterator<Connection> iterator = new HashSet<>(connections).iterator();
                iterator.hasNext(); ) {
            Connection connection = iterator.next();
            connection.interrupt();
            connection.releaseResources();
            iterator.remove();
        }
        connections.clear();
    }

    /**
     * Main routing method
     */
    private void handleRequest(Request request, InputStream in, OutputStream out)
            throws IOException {
        //malformed request
        if (!request.isValid()) {
            out.write(BAD_REUEST_RESPONSE.getBytes());
            out.flush();
        }

        Socket outSocket = SocketChannel.open().socket();
        if (vpnService != null) {
            vpnService.protect(outSocket);
        }
        SocketAddress socketAddress = new InetSocketAddress(request.getHost(), 80);
        outSocket.connect(socketAddress);

        OutputStream serverOut = outSocket.getOutputStream();
        InputStream serverIn = outSocket.getInputStream();

        Thread fromServerPipe = new Thread(new PipeRunnable(serverIn, out));
        fromServerPipe.start();
        //write actual request to server
        request.writeTo(serverOut);
        try {
            fromServerPipe.join();
        } catch (InterruptedException e) {
        } finally {
            outSocket.close();
        }
    }


    private class Connection extends Thread {

        private Socket socket;

        private InputStream in;

        private OutputStream out;

        public Connection(Socket socket) {
            this.socket = socket;
            setDaemon(true);
        }

        public void run() {
            connections.add(this);
            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
                Request request = Request.read(in);
                handleRequest(request, in, out);
                connections.remove(this);
            } catch (IOException e) {
                Log.w(TAG, "can't handle connection", e);
            } finally {
                releaseResources();
            }
        }

        private void releaseResources() {
            try {
                if (in != null) in.close();
            } catch (Exception e) {}
            try {
                if (out != null) out.close();
            } catch (Exception e) {}
            try {
                socket.close();
            } catch (Exception e) {}
        }

    }

}
