package com.example.server.notice;

import com.example.common.error.BusinessException;
import com.example.common.error.CommonErrorCode;

public class NoticeStateException extends BusinessException {

    public NoticeStateException(String message) {
        super(CommonErrorCode.INVALID_REQUEST, message);
    }
}
