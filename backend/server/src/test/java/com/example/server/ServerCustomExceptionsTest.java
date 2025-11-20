package com.example.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.file.FileStorageException;
import com.example.file.StoredFileNotFoundException;
import com.example.server.notice.NoticeNotFoundException;
import com.example.server.notice.NoticeStateException;
import com.example.server.notification.UserNotificationNotFoundException;

@DisplayName("서버 커스텀 예외 테스트")
class ServerCustomExceptionsTest {

    @Test
    @DisplayName("Given 알림 ID When 예외 생성 Then 식별자가 메시지에 포함된다")
    void userNotificationNotFoundExceptionContainsIdentifier() {
        UUID id = UUID.randomUUID();

        UserNotificationNotFoundException exception = new UserNotificationNotFoundException(id);

        assertThat(exception)
                .hasMessageContaining(id.toString());
    }

    @Test
    @DisplayName("Given 공지 예외 When 생성하면 Then 맥락 정보가 메시지에 포함된다")
    void noticeExceptionsDescribeContext() {
        UUID noticeId = UUID.randomUUID();
        NoticeNotFoundException notFoundException = new NoticeNotFoundException(noticeId);
        NoticeStateException stateException = new NoticeStateException("잘못된 상태");

        assertThat(notFoundException).hasMessageContaining("공지사항").hasMessageContaining(noticeId.toString());
        assertThat(stateException).hasMessageContaining("잘못된 상태");
    }

    @Test
    @DisplayName("Given 파일 예외 When 생성하면 Then 원인과 메시지가 유지된다")
    void fileExceptionsPreserveMessageAndCause() {
        UUID fileId = UUID.randomUUID();
        StoredFileNotFoundException notFoundException = new StoredFileNotFoundException(fileId);
        RuntimeException cause = new RuntimeException("root");
        FileStorageException storageException = new FileStorageException("저장 실패", cause);

        assertThat(notFoundException).hasMessageContaining(fileId.toString());
        assertThat(storageException)
                .hasMessageContaining("저장 실패")
                .hasCause(cause);
    }
}
