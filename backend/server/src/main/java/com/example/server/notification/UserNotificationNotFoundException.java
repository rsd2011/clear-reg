package com.example.server.notification;

import java.util.UUID;

import com.example.common.error.BusinessException;
import com.example.common.error.CommonErrorCode;

public class UserNotificationNotFoundException extends BusinessException {

    public UserNotificationNotFoundException(UUID id) {
        super(CommonErrorCode.NOT_FOUND, "알림을 찾을 수 없습니다: " + id);
    }
}
