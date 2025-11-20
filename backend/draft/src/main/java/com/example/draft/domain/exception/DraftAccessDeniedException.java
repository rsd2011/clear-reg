package com.example.draft.domain.exception;

public class DraftAccessDeniedException extends RuntimeException {

    public DraftAccessDeniedException(String message) {
        super(message);
    }
}
