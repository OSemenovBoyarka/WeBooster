package com.uawebchallenge.webooster.http;

import com.uawebchallenge.webooster.util.EncoderUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

/**
 * Helper class, used to modify requests for use with google data compression proxy
 *
 * see https://code.google.com/p/datacompressionproxy/source/browse/background.js
 *
 * @author Alexander Semenov
 */
public class DataCompressionProxyHelper {
   
    private static final String HTTP_HOST = "compress.googlezip.net";
    private static final String HTTPS_HOST = "proxy.googlezip.net";
    private static final int HTTP_PORT = 80;
    private static final int HTTPS_PORT = 443;

    private static final String AUTH_KEY = "ac4500dd3b7579186c1b0620614fdb1f7d61f944";

    private static final SocketFactory SSL_SOCKET_FACTORY = SSLSocketFactory.getDefault();

    static void connectSocketToProxy(Socket socket) throws IOException {
        socket.connect(new InetSocketAddress("compress.googlezip.net", 80));
    }


    static void modifyRequest(RequestModel request){
        long timestamp = System.currentTimeMillis();
        String sid = EncoderUtil.generateMD5(timestamp + AUTH_KEY + timestamp);
        String key = "ps=" + timestamp + "-" + randomMagicLong() + "-" + randomMagicLong() + "-" + randomMagicLong() + ", sid=" + sid + ", b=2228, p=0, c=win";
        request.setHeader("Chrome-Proxy", key);
    }


    private static long randomMagicLong() {
        return (long) Math.floor(Math.random() * 1000000000);
    }

}
