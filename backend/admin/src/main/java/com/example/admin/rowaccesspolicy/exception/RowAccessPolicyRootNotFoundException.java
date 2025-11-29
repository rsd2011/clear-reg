package com.example.admin.rowaccesspolicy.exception;

/**
 * 행 접근 정책 루트를 찾을 수 없을 때 발생하는 예외.
 */
public class RowAccessPolicyRootNotFoundException extends RuntimeException {

    public RowAccessPolicyRootNotFoundException(String message) {
        super(message);
    }

    public RowAccessPolicyRootNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
