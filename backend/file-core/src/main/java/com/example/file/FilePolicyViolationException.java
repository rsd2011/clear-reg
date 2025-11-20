package com.example.file;

public class FilePolicyViolationException extends RuntimeException {

    public FilePolicyViolationException(String message) {
        super(message);
    }

    public FilePolicyViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
