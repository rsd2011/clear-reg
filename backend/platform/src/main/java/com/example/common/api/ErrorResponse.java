package com.example.common.api;

public record ErrorResponse(String code, String message, String traceId, String timestamp) {
    public ErrorResponse(String code, String message) {
        this(code, message, null, null);
    }
}
