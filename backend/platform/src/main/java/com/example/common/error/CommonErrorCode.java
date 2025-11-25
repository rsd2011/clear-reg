package com.example.common.error;

public enum CommonErrorCode implements ErrorCode {
    INVALID_REQUEST("INVALID_REQUEST", "잘못된 요청입니다."),
    NOT_FOUND("NOT_FOUND", "리소스를 찾을 수 없습니다."),
    PERMISSION_DENIED("PERMISSION_DENIED", "권한이 없습니다."),
    CONFLICT("CONFLICT", "처리 중 충돌이 발생했습니다."),
    INTERNAL_ERROR("INTERNAL_ERROR", "예상하지 못한 오류가 발생했습니다.");

    private final String code;
    private final String message;

    CommonErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
