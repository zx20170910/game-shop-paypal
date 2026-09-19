package com.unicorn.gameshop.common;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final ApiErrorCode code;
    private final HttpStatus status;
    private final boolean retryable;

    public BusinessException(ApiErrorCode code, String message) {
        this(code, message, HttpStatus.BAD_REQUEST, false);
    }

    public BusinessException(ApiErrorCode code, String message, HttpStatus status, boolean retryable) {
        super(message);
        this.code = code;
        this.status = status;
        this.retryable = retryable;
    }

    public ApiErrorCode getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
