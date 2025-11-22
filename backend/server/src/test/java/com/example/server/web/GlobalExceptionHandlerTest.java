package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.example.auth.InvalidCredentialsException;
import com.example.file.FilePolicyViolationException;
import com.example.file.FileStorageException;
import com.example.file.StoredFileNotFoundException;
import com.example.server.notice.NoticeStateException;
import com.example.server.notice.NoticeNotFoundException;
import com.example.server.notification.UserNotificationNotFoundException;

class GlobalExceptionHandlerTest {

    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("잘못된 자격 증명은 401과 메시지를 반환한다")
    void invalidCredentials() {
        ResponseEntity<GlobalExceptionHandler.ProblemResponse> response =
                handler.handleInvalidCredentials(new InvalidCredentialsException());

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody().message()).contains("Invalid username or password");
    }

    @Test
    @DisplayName("저장된 파일을 찾지 못하면 404와 메시지를 반환한다")
    void storedFileNotFound() {
        ResponseEntity<GlobalExceptionHandler.ProblemResponse> response =
                handler.handleFileNotFound(new StoredFileNotFoundException(java.util.UUID.randomUUID()));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().message()).contains("찾을 수 없습니다");
    }

    @Test
    @DisplayName("공지사항을 찾지 못하면 404와 메시지를 반환한다")
    void noticeNotFound() {
        ResponseEntity<GlobalExceptionHandler.ProblemResponse> response =
                handler.handleNoticeNotFound(new NoticeNotFoundException(java.util.UUID.randomUUID()));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().message()).contains("공지사항을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("파일 저장소 오류 시 500을 반환한다")
    void fileStorageException() {
        ResponseEntity<GlobalExceptionHandler.ProblemResponse> response =
                handler.handleFileStorage(new FileStorageException("저장 실패", new java.io.IOException("io")));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().message()).contains("저장 실패");
    }

    @Test
    @DisplayName("파일 정책 위반은 400을 반환한다")
    void filePolicyViolation() {
        ResponseEntity<GlobalExceptionHandler.ProblemResponse> response =
                handler.handleFilePolicy(new FilePolicyViolationException("위반"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().message()).contains("위반");
    }

    @Test
    @DisplayName("공지 상태 예외는 400을 반환한다")
    void noticeState() {
        ResponseEntity<GlobalExceptionHandler.ProblemResponse> response =
                handler.handleNoticeState(new NoticeStateException("상태 오류"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().message()).contains("상태 오류");
    }

    @Test
    @DisplayName("알림을 찾지 못하면 404를 반환한다")
    void notificationNotFound() {
        ResponseEntity<GlobalExceptionHandler.ProblemResponse> response =
                handler.handleNotificationNotFound(new UserNotificationNotFoundException(java.util.UUID.randomUUID()));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().message()).contains("알림");
    }

    @Test
    @DisplayName("검증 오류 시 첫 필드 메시지를 반환한다")
    void validation_returnsFieldMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        org.springframework.validation.BeanPropertyBindingResult errors =
                new org.springframework.validation.BeanPropertyBindingResult(new Object(), "target");
        errors.addError(new org.springframework.validation.FieldError("target", "name", "must not be blank"));
        ResponseEntity<GlobalExceptionHandler.ProblemResponse> response =
                handler.handleValidation(new org.springframework.web.bind.MethodArgumentNotValidException(null, errors));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().message()).contains("name must not be blank");
    }
}
