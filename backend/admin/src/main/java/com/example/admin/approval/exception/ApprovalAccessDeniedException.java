package com.example.admin.approval.exception;

import com.example.common.error.BusinessException;
import com.example.common.error.CommonErrorCode;

public class ApprovalAccessDeniedException extends BusinessException {
    public ApprovalAccessDeniedException(String message) {
        super(CommonErrorCode.PERMISSION_DENIED, message);
    }
}
