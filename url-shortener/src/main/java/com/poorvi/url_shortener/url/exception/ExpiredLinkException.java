
package com.poorvi.url_shortener.url.exception;

public class ExpiredLinkException extends RuntimeException {

    public ExpiredLinkException(String shortCode) {
        super("Short URL has expired: " + shortCode);
    }
}
