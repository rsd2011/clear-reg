package com.example.file;

import com.example.common.error.BusinessException;
import com.example.common.error.CommonErrorCode;

public class FileStorageException extends BusinessException {

    public FileStorageException(String message, Throwable cause) {
        super(CommonErrorCode.INTERNAL_ERROR, message, cause);
    }
}
