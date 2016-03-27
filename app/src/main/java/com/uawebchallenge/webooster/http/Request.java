package com.uawebchallenge.webooster.http;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.uawebchallenge.webooster.http.HttpConstants.CR;
import static com.uawebchallenge.webooster.http.HttpConstants.EOF;
import static com.uawebchallenge.webooster.http.HttpConstants.HEADER_CONTENT_LENGTH;
import static com.uawebchallenge.webooster.http.HttpConstants.HEADER_HOST;
import static com.uawebchallenge.webooster.http.HttpConstants.LF;

public class Request {



    private Request() {}

    private List<String> lines = new ArrayList<>();
    private Map<String, String> headers = new HashMap<>();
    private String host;
    private String method;
    private String uri;
    private int contentLength = -1;

    /**
     * @return raw request header part
     */
    @NonNull
    public List<String> getLines() {
        return lines;
    }

    @NonNull
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

    public boolean hasBody() {
        return contentLength >= 0;
    }

    boolean isValid(){
        return !(TextUtils.isEmpty(uri) && TextUtils.isEmpty(method) && TextUtils.isEmpty(host));
    }

    /**
     * Parses HTTP/1.1 request from given input stream.
     *
     * This parses only headers, if request has content, it shuold be read separately from the same input stream
     *
     */
    public static Request read(InputStream is) throws IOException {
        Request request = new Request();
        String s = readLine(is);
        //obtain method and uri
        if (s != null && s.contains("HTTP/")){
            String[] split = s.split(" ");
            if (split.length < 2){
                throw new BadRequestException("Malformed first line of request: "+s);
            }
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

    private void extractHeaders() throws IOException {
        for (String header : lines){
            String[] split = header.split(":");
            if (split.length != 2){
                //malformed header
                continue;
            }
            String headerName = split[0].trim();
            String headerValue = split[1].trim();
            headers.put(headerName, headerValue);
            if (host == null && headerName.equalsIgnoreCase(HEADER_HOST)){
                host = headerValue;
            } else if (contentLength < 0 && headerName.equalsIgnoreCase(HEADER_CONTENT_LENGTH)){
                try {
                    contentLength = Integer.parseInt(headerValue);
                } catch (NumberFormatException e){
                    throw new BadRequestException("Malformed Content-Length header value: "+headerValue);
                }
            }
        }
    }

    /**
     * Reads single line from input stream, handling CRLF symbols
     *
     * Based on https://github.com/miku-nyan/DCP-bridge/blob/master/src/nya/miku/dcpbridge/Request.java
     */
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

