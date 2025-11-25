package com.example.server.notice;

import java.util.UUID;

import com.example.common.error.BusinessException;
import com.example.common.error.CommonErrorCode;

public class NoticeNotFoundException extends BusinessException {

    public NoticeNotFoundException(UUID id) {
        super(CommonErrorCode.NOT_FOUND, "공지사항을 찾을 수 없습니다: " + id);
    }
}
