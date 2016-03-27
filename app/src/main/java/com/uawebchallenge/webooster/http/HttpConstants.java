package com.uawebchallenge.webooster.http;

import java.nio.charset.Charset;

/**
 * @author Alexander Semenov
 */
public class HttpConstants {

    static final char CR  = 13, LF  = 10;
    static final int EOF  = -1;
    static final String HEADER_HOST = "Host";
    static final String HEADER_CONTENT_LENGTH = "Content-Length";

    static final String HTTP_VERSION_1_1 = "HTTP/1.1";


    static final Charset HTTP_CHARSET;
    static {
        Charset charset;
        try {
            charset = Charset.forName("UTF-8");
        } catch (Exception e){
            charset = Charset.defaultCharset();
        }
        HTTP_CHARSET = charset;
    }

    static final String CRLF_STR =  new String(new char[]{CR,LF});
    static final byte[] CRLF = CRLF_STR.getBytes(HTTP_CHARSET);

}


