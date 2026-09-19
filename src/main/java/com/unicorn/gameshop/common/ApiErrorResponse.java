package com.unicorn.gameshop.common;

public record ApiErrorResponse(String code, String message, String requestId, boolean retryable) {
}
