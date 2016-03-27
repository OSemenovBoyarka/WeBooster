package com.uawebchallenge.webooster.http;

import com.uawebchallenge.webooster.util.StringUtils;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.uawebchallenge.webooster.http.HttpConstants.CRLF_STR;
import static com.uawebchallenge.webooster.http.HttpConstants.HTTP_VERSION_1_1;

/**
 * Simple proxy server for plain HTTP traffic, doesn't support HTTPS, will return BAD REQUEST for
 * all
 * https attemts
 *
 * Main traffic compression controller for the whole application
 *
 * @author Alexander Semenov
 */
@SuppressWarnings("EmptyCatchBlock")
public class HttpCompressingProxyServer {

    private static final String TAG = HttpCompressingProxyServer.class.getSimpleName();

    //TODO add nice html here
    public static final String BAD_REQUEST_RESPONSE = HTTP_VERSION_1_1+" 400 Bad Request"+CRLF_STR;

    private volatile boolean listening;

    private Set<Connection> connections = Collections.synchronizedSet(new HashSet<Connection>());

    private ServerSocket listenSocket;

    private Proxy chainingProxy;

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
            Log.d(TAG, "Http proxy running on port "+port);
        } catch (IOException e) {
            throw new IOException("Can't start local proxy on port " + port, e);
        }
        while (listening) {
            try {
                Connection connection = new Connection(listenSocket.accept());
                connection.start();
            } catch (IOException e) {
                Log.w(TAG, "Failed to proxy request: " + e);
            }
        }
    }

    public synchronized void stop() {
        if (!listening) {
            return;
        }
        listening = false;
        try {
            if (listenSocket != null) {
                listenSocket.close();
            }
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
        listenSocket = null;
        connections.clear();


    }

    /**
     * Main routing method
     */
    protected void handleRequest(Request request, InputStream fromClientIn, OutputStream toClientOut)
            throws IOException {
        Log.i(TAG, "Received " + request.getMethod() + " request to " + request.getHost());

        URL url = new URL("http", request.getHost(), request.getUri());
        HttpURLConnection connection;
        if (chainingProxy != null) {
            connection = (HttpURLConnection) url.openConnection(chainingProxy);
        } else {
            connection = (HttpURLConnection) url.openConnection();
        }

        connection.setRequestMethod(request.getMethod());
        for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
            connection.setRequestProperty(header.getKey(), header.getValue());
        }
        connection.setRequestMethod(request.getMethod());
        connection.setDoOutput(request.hasBody());
        connection.connect();
        //pipe request body to connection
        if (request.hasBody()){
            Thread serverToClient = new PipeThread(fromClientIn, connection.getOutputStream());
            serverToClient.start();
            try {
                serverToClient.join();
            } catch (InterruptedException e) {
                Log.i(TAG, "data send stream interrupted: "+e.getMessage());
                return;
            }
        }

        //constructing response
        Response response = new Response(connection.getResponseCode(), connection.getResponseMessage());
        Map<String, List<String>> responseHeaders = connection.getHeaderFields();
        for (Map.Entry<String, List<String>> header: responseHeaders.entrySet()){
            String headerValue = StringUtils.join(header.getValue(), ",");
            String headerName = header.getKey();
            //null means status line
            if (headerName == null){
                response.setStatusLine(headerValue);
            } else {
                response.addHeader(headerName,headerValue);
            }
        }
        response.setContentLength(connection.getContentLength());

        response.writeTo(toClientOut);

        Thread serverToClient = new PipeThread(connection.getInputStream(), toClientOut);
        serverToClient.start();
        try {
            serverToClient.join();
        } catch (InterruptedException e) {
            Log.i(TAG, "data receive stream interrupted: "+e.getMessage());
        }
    }

    public void setChainingProxy(Proxy chainingProxy) {
        this.chainingProxy = chainingProxy;
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

                Request request = parseRequest();
                if (request != null) {
                    handleRequest(request, in, out);
                }

                connections.remove(this);
            } catch (IOException e) {
                Log.w(TAG, "can't handle connection", e);
            } finally {
                releaseResources();
            }
        }

        private Request parseRequest() throws IOException {
            try {
                Request request = Request.read(in);
                if (request.isValid()) {
                    return request;
                }
            } catch (BadRequestException e) {
                Log.w(TAG, "Got malformed request", e);
            }
            out.write(BAD_REQUEST_RESPONSE.getBytes());
            out.flush();
            return null;
        }

        private void releaseResources() {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
            }
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
            }
            try {
                socket.close();
            } catch (Exception e) {
            }
        }

    }

}
