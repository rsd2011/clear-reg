package com.example.draft.domain.exception;

public class DraftNotFoundException extends RuntimeException {

    public DraftNotFoundException(String message) {
        super(message);
    }
}
