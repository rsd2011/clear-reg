package com.example.file;

import com.example.common.error.BusinessException;
import com.example.common.error.CommonErrorCode;

public class FilePolicyViolationException extends BusinessException {

    public FilePolicyViolationException(String message) {
        super(CommonErrorCode.INVALID_REQUEST, message);
    }

    public FilePolicyViolationException(String message, Throwable cause) {
        super(CommonErrorCode.INVALID_REQUEST, message, cause);
    }
}
