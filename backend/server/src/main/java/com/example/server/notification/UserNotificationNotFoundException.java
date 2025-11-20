package com.example.server.notification;

import java.util.UUID;

public class UserNotificationNotFoundException extends RuntimeException {

    public UserNotificationNotFoundException(UUID id) {
        super("알림을 찾을 수 없습니다: " + id);
    }
}
