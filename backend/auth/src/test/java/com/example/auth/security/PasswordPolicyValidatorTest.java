package com.example.auth.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.config.AuthPolicyProperties;

@DisplayName("PasswordPolicyValidator")
class PasswordPolicyValidatorTest {

    private final PasswordPolicyValidator validator;

    PasswordPolicyValidatorTest() {
        AuthPolicyProperties properties = new AuthPolicyProperties();
        properties.setPasswordMinLength(6);
        validator = new PasswordPolicyValidator(properties);
    }

    @Test
    @DisplayName("Given weak password When validate Then throw")
    void givenWeakPasswordWhenValidateThenThrow() {
        assertThatThrownBy(() -> validator.validate("short"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Given strong password When validate Then pass")
    void givenStrongPasswordWhenValidateThenPass() {
        assertThatCode(() -> validator.validate("Abcd!2345"))
                .doesNotThrowAnyException();
    }
}
