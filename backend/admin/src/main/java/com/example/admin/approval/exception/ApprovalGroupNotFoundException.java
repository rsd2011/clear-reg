package com.example.admin.approval.exception;

public class ApprovalGroupNotFoundException extends RuntimeException {

    public ApprovalGroupNotFoundException(String message) {
        super(message);
    }

    public ApprovalGroupNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
