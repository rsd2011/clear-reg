package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.example.auth.InvalidCredentialsException;

@DisplayName("GlobalExceptionHandler 테스트")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Given 잘못된 자격 증명 When 처리하면 Then 401 응답을 반환한다")
    void givenInvalidCredentialsWhenHandledThenUnauthorized() {
        InvalidCredentialsException exception = new InvalidCredentialsException();

        ResponseEntity<GlobalExceptionHandler.ProblemResponse> response = handler.handleInvalidCredentials(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody().message()).contains("Invalid");
    }

    @Test
    @DisplayName("Given 검증 실패 When 처리하면 Then 400 응답과 필드 정보를 반환한다")
    void givenValidationFailureWhenHandledThenBadRequest() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "username", "is required"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<GlobalExceptionHandler.ProblemResponse> response = handler.handleValidation(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().message()).contains("username");
    }

}
