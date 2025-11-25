package com.example.file;

import java.util.UUID;

import com.example.common.error.BusinessException;
import com.example.common.error.CommonErrorCode;

public class StoredFileNotFoundException extends BusinessException {

    public StoredFileNotFoundException(UUID id) {
        super(CommonErrorCode.NOT_FOUND, "파일을 찾을 수 없습니다: " + id);
    }
}
