package com.example.server.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.auth.InvalidCredentialsException;
import com.example.file.FilePolicyViolationException;
import com.example.file.FileStorageException;
import com.example.file.StoredFileNotFoundException;
import com.example.common.api.dto.ErrorResponse;
import com.example.common.error.BusinessException;
import com.example.common.error.CommonErrorCode;
import com.example.common.error.ErrorCode;
import com.example.server.notice.NoticeNotFoundException;
import com.example.server.notice.NoticeStateException;
import com.example.server.notification.UserNotificationNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemResponse> handleInvalidCredentials(InvalidCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ProblemResponse(exception.getMessage()));
    }

    @ExceptionHandler(NoticeNotFoundException.class)
    public ResponseEntity<ProblemResponse> handleNoticeNotFound(NoticeNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ProblemResponse(exception.getMessage()));
    }

    @ExceptionHandler(NoticeStateException.class)
    public ResponseEntity<ProblemResponse> handleNoticeState(NoticeStateException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ProblemResponse(exception.getMessage()));
    }

    @ExceptionHandler(UserNotificationNotFoundException.class)
    public ResponseEntity<ProblemResponse> handleNotificationNotFound(UserNotificationNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ProblemResponse(exception.getMessage()));
    }

    @ExceptionHandler(StoredFileNotFoundException.class)
    public ResponseEntity<ProblemResponse> handleFileNotFound(StoredFileNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ProblemResponse(exception.getMessage()));
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ProblemResponse> handleFileStorage(FileStorageException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ProblemResponse(exception.getMessage()));
    }

    @ExceptionHandler(FilePolicyViolationException.class)
    public ResponseEntity<ProblemResponse> handleFilePolicy(FilePolicyViolationException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ProblemResponse(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Invalid request");
        return ResponseEntity.badRequest().body(new ProblemResponse(message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ProblemResponse(exception.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException exception) {
        ErrorCode code = exception.errorCode();
        HttpStatus status = toStatus(code);
        return ResponseEntity.status(status)
                .body(new ErrorResponse(code.code(), exception.getMessage()));
    }

    private HttpStatus toStatus(ErrorCode code) {
        if (code == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        if (code == CommonErrorCode.PERMISSION_DENIED) return HttpStatus.FORBIDDEN;
        if (code == CommonErrorCode.NOT_FOUND) return HttpStatus.NOT_FOUND;
        if (code == CommonErrorCode.CONFLICT) return HttpStatus.CONFLICT;
        if (code == CommonErrorCode.INVALID_REQUEST) return HttpStatus.BAD_REQUEST;
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public record ProblemResponse(String message) {
    }
}
