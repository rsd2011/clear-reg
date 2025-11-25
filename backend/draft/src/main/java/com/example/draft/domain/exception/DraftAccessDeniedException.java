package com.example.draft.domain.exception;

import com.example.common.error.BusinessException;
import com.example.common.error.CommonErrorCode;

public class DraftAccessDeniedException extends BusinessException {

    public DraftAccessDeniedException(String message) {
        super(CommonErrorCode.PERMISSION_DENIED, message);
    }
}
