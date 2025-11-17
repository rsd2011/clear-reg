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
    private final PolicyToggleProvider toggleProvider;

    PasswordPolicyValidatorTest() {
        AuthPolicyProperties properties = new AuthPolicyProperties();
        properties.setPasswordMinLength(6);
        toggleProvider = new PolicyToggleProvider() {
            @Override
            public boolean isPasswordPolicyEnabled() {
                return true;
            }

            @Override
            public boolean isPasswordHistoryEnabled() {
                return true;
            }

            @Override
            public boolean isAccountLockEnabled() {
                return true;
            }

            @Override
            public java.util.List<com.example.auth.LoginType> enabledLoginTypes() {
                return java.util.List.of(com.example.auth.LoginType.values());
            }
        };
        validator = new PasswordPolicyValidator(properties, toggleProvider);
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

    @Test
    @DisplayName("Given disabled policy When validate Then skip checks")
    void givenDisabledPolicyWhenValidateThenSkip() {
        PolicyToggleProvider disabled = new PolicyToggleProvider() {
            @Override
            public boolean isPasswordPolicyEnabled() {
                return false;
            }

            @Override
            public boolean isPasswordHistoryEnabled() {
                return false;
            }

            @Override
            public boolean isAccountLockEnabled() {
                return true;
            }

            @Override
            public java.util.List<com.example.auth.LoginType> enabledLoginTypes() {
                return java.util.List.of(com.example.auth.LoginType.values());
            }
        };
        PasswordPolicyValidator disabledValidator = new PasswordPolicyValidator(new AuthPolicyProperties(), disabled);

        assertThatCode(() -> disabledValidator.validate(null)).doesNotThrowAnyException();
    }
}
