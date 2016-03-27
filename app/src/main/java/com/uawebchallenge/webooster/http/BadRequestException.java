package com.uawebchallenge.webooster.http;

import java.io.IOException;

/**
 * @author Alexander Semenov
 */
public class BadRequestException extends IOException {

    public BadRequestException() {
    }

    public BadRequestException(String detailMessage) {
        super(detailMessage);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadRequestException(Throwable cause) {
        super(cause);
    }
}
