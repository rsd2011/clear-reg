package com.example.draft.domain.exception;

import com.example.common.error.BusinessException;
import com.example.common.error.CommonErrorCode;

public class DraftNotFoundException extends BusinessException {

    public DraftNotFoundException(String message) {
        super(CommonErrorCode.NOT_FOUND, message);
    }
}
