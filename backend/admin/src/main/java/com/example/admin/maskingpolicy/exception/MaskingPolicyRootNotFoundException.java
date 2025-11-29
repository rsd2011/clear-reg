package com.example.admin.maskingpolicy.exception;

/**
 * 마스킹 정책을 찾을 수 없을 때 발생하는 예외.
 */
public class MaskingPolicyRootNotFoundException extends RuntimeException {

    public MaskingPolicyRootNotFoundException(String message) {
        super(message);
    }

    public MaskingPolicyRootNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
