/*
 * Data Compression Proxy bridge for Overchan
 * Copyright (C) 2014-2015  miku-nyan <https://github.com/miku-nyan>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.uawebchallenge.webooster.http;

import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Based on https://github.com/miku-nyan/DCP-bridge/blob/master/src/nya/miku/dcpbridge/Request.java
 */
public class Request {
    private static final byte[] CRLF = new byte[] { '\r', '\n' };

    private Request() {}

    private List<String> lines = new ArrayList<>();
    private Map<String, String> headers = new HashMap<>();
    private String host;
    private String method;
    private String uri;

    public List<String> getLines() {
        return lines;
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public String getHost() {
        return host;
    }

    public String getUri() {
        return uri;
    }

    public String getMethod() {
        return method;
    }

    boolean isValid(){
        return !(TextUtils.isEmpty(uri) && TextUtils.isEmpty(method) && TextUtils.isEmpty(host));
    }

    public void writeTo(OutputStream out) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (String line : lines) {
            baos.write(line.getBytes("UTF-8"));
            baos.write(CRLF);
        }
        baos.write(CRLF);
        baos.writeTo(out);
    }

    public static Request read(InputStream is) throws IOException {
        Request request = new Request();
        String s = readLine(is);
        //obtain method and uri
        if (s != null && s.contains("HTTP/")){
            String[] split = s.split(" ");
            request.method = split[0];
            request.uri = split[1];
        }
        while (s != null && s.length() > 0) {
            request.lines.add(s);
            s = readLine(is);
        }

        request.extractHeaders();

        return request;
    }

    private void extractHeaders() {
        for (String header : lines){
            String[] split = header.split(":");
            if (split.length != 2){
                //malformed header
                continue;
            }
            String headerName = split[0].trim();
            String headerValue = split[1].trim();
            headers.put(headerName, headerValue);
            if (headerName.equalsIgnoreCase("Host")){
                host = headerValue;
            }
        }
    }

    private static final char CR  = 13, LF  = 10;
    private static final int EOF  = -1;

    private static String readLine(InputStream is) throws IOException {
        StringBuilder line = new StringBuilder();
        int i;
        char c;
        i = is.read();
        if (i == EOF) return null;
        while (i > EOF && i != LF && i != CR) {
            c = (char)(i & 0xFF);
            line = line.append(c);
            i = is.read();
        }

        if (i == CR) {
            //noinspection ResultOfMethodCallIgnored
            is.read();
        }
        return line.toString();
    }
}

