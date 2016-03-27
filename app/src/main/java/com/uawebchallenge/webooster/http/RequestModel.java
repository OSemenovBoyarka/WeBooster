package com.uawebchallenge.webooster.http;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.uawebchallenge.webooster.http.HttpConstants.CR;
import static com.uawebchallenge.webooster.http.HttpConstants.CRLF;
import static com.uawebchallenge.webooster.http.HttpConstants.CRLF_STR;
import static com.uawebchallenge.webooster.http.HttpConstants.EOF;
import static com.uawebchallenge.webooster.http.HttpConstants.HEADER_CONTENT_LENGTH;
import static com.uawebchallenge.webooster.http.HttpConstants.HEADER_HOST;
import static com.uawebchallenge.webooster.http.HttpConstants.HTTP_CHARSET;
import static com.uawebchallenge.webooster.http.HttpConstants.HTTP_VERSION_1_1;
import static com.uawebchallenge.webooster.http.HttpConstants.LF;

public class RequestModel {



    private RequestModel() {}

    private Map<String, String> headers = new HashMap<>();
    private String host;
    private String method;
    private String uri;
    private String requestLine;
    private int contentLength = -1;

    @NonNull
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public void setHeader(String name, String value){
        headers.put(name, value);
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

    public String getRequestLine() {
        return requestLine;
    }

    boolean isValid(){
        return !(TextUtils.isEmpty(uri) && !TextUtils.isEmpty(method) && !TextUtils.isEmpty(host));
    }

    /**
     * Parses HTTP/1.1 request from given input stream.
     *
     * This parses only headers, if request has content, it shuold be read separately from the same input stream
     *
     */
    public static RequestModel read(InputStream is) throws IOException {
        RequestModel request = new RequestModel();
        List<String> lines = new ArrayList<>();
        String s = readLine(is);
        request.requestLine = s;
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
            lines.add(s);
            s = readLine(is);
        }

        request.extractHeaders(lines);

        return request;
    }

    private void extractHeaders(List<String> lines) throws IOException {
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

    public void writeTo(OutputStream out) throws IOException{
        String requestLine;
        if (TextUtils.isEmpty(this.requestLine)){
            requestLine = method+" "+uri+" "+HTTP_VERSION_1_1+CRLF_STR;
        } else {
            requestLine = this.requestLine;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(requestLine.getBytes(HTTP_CHARSET));
        baos.write(CRLF);
        for (Map.Entry<String, String> header : headers.entrySet()) {
            String line = header.getKey()+": "+header.getValue();
            baos.write(line.getBytes(HTTP_CHARSET));
            //CRLF on end of each header
            baos.write(CRLF);
        }
        //CRLF, indicating header section is ended
        baos.write(CRLF);
        baos.writeTo(out);
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

    @Override
    public String toString() {
        return "RequestModel{" +
                "headers=" + headers +
                ", host='" + host + '\'' +
                ", method='" + method + '\'' +
                ", uri='" + uri + '\'' +
                ", requestLine='" + requestLine + '\'' +
                ", contentLength=" + contentLength +
                '}';
    }
}

