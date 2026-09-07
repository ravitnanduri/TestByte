package com.testbyte.backend.exception;

import org.springframework.http.HttpStatus;

public class GoneException extends ApiException {
    public GoneException(String message) {
        super(HttpStatus.GONE, message);
    }
}
