package com.example.server.notice;

import java.util.UUID;

public class NoticeNotFoundException extends RuntimeException {

    public NoticeNotFoundException(UUID id) {
        super("공지사항을 찾을 수 없습니다: " + id);
    }
}
