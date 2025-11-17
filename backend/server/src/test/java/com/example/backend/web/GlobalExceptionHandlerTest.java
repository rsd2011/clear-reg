package com.example.backend.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.example.auth.InvalidCredentialsException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Given invalid credentials When handled Then returns 401")
    void givenInvalidCredentialsWhenHandledThenUnauthorized() {
        InvalidCredentialsException exception = new InvalidCredentialsException();

        ResponseEntity<GlobalExceptionHandler.ProblemResponse> response = handler.handleInvalidCredentials(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody().message()).contains("Invalid");
    }

    @Test
    @DisplayName("Given validation failure When handled Then returns 400")
    void givenValidationFailureWhenHandledThenBadRequest() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "username", "is required"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<GlobalExceptionHandler.ProblemResponse> response = handler.handleValidation(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().message()).contains("username");
    }

}
