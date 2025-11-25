package com.example.draft.domain.exception;

import com.example.common.error.BusinessException;
import com.example.common.error.CommonErrorCode;

public class DraftWorkflowException extends BusinessException {

    public DraftWorkflowException(String message) {
        super(CommonErrorCode.CONFLICT, message);
    }
}
