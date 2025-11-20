package com.example.draft.domain.exception;

public class DraftTemplateNotFoundException extends RuntimeException {

    public DraftTemplateNotFoundException(String message) {
        super(message);
    }
}
