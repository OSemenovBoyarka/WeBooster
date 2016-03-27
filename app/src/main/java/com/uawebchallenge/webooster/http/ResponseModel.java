package com.uawebchallenge.webooster.http;

import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static com.uawebchallenge.webooster.http.HttpConstants.CRLF;
import static com.uawebchallenge.webooster.http.HttpConstants.CRLF_STR;
import static com.uawebchallenge.webooster.http.HttpConstants.HTTP_CHARSET;
import static com.uawebchallenge.webooster.http.HttpConstants.HTTP_VERSION_1_1;
/**
 * Class, represents http response. Only header values, body should be handled separately
 *
 * @author Alexander Semenov
 */
public class ResponseModel {

    private final Map<String, String> headers  = new HashMap<>();
    private String httpVersion;
    private final int statusCode;
    private String statusMessage;
    private String statusLine;
    private int contentLength = -1;

    //region Construction
    public ResponseModel(String httpVersion, int statusCode, String statusMessage) {
        this(statusCode, statusMessage);
        this.httpVersion = httpVersion;
    }

    public ResponseModel(int statusCode, String statusMessage) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.httpVersion = HTTP_VERSION_1_1;
    }
    //endregion

    //region Member access
    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public int getContentLength() {
        return contentLength;
    }

    public String getStatusLine() {
        return statusLine;
    }

    public ResponseModel setContentLength(int contentLength) {
        this.contentLength = contentLength;
        return this;
    }

    public ResponseModel setStatusLine(String statusLine) {
        this.statusLine = statusLine;
        return this;
    }

    public ResponseModel addHeader(String name, String value){
        headers.put(name, value);
        return this;
    }

    public ResponseModel addHeaders(Map<String, String> headers){
        this.headers.putAll(headers);
        return this;
    }
    //endregion

    public boolean isSuccess(){
        //all 200+ codes are successful
        return statusCode/100 == 2;
    }

    public void writeTo(OutputStream out) throws IOException{
        String statusLine;
        if (TextUtils.isEmpty(this.statusLine)){
            statusLine = HTTP_VERSION_1_1+" "+statusCode+" "+statusMessage+CRLF_STR;
        } else {
            statusLine = this.statusLine;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(statusLine.getBytes(HTTP_CHARSET));
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
}
