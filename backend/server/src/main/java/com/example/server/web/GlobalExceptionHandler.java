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

    public record ProblemResponse(String message) {
    }
}
