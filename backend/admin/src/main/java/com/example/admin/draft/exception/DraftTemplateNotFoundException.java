package com.example.admin.draft.exception;

import com.example.common.error.BusinessException;
import com.example.common.error.CommonErrorCode;

public class DraftTemplateNotFoundException extends BusinessException {

    public DraftTemplateNotFoundException(String message) {
        super(CommonErrorCode.NOT_FOUND, message);
    }
}
