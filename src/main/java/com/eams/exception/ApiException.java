package com.eams.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for expected business errors.
 * Each subclass maps to one HTTP status, so services never
 * need to know about HTTP and the client always gets the right code.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
