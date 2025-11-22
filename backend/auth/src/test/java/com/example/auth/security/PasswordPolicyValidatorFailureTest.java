package com.example.auth.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.config.AuthPolicyProperties;

@DisplayName("PasswordPolicyValidator 실패 분기")
class PasswordPolicyValidatorFailureTest {

    @Test
    @DisplayName("약한 비밀번호는 InvalidCredentialsException을 던진다")
    void weakPasswordThrows() {
        AuthPolicyProperties props = new AuthPolicyProperties();
        props.setPasswordMinLength(8);
        props.setRequireDigit(true);
        props.setRequireUppercase(true);
        props.setRequireLowercase(true);
        props.setRequireSpecial(true);
        PolicyToggleProvider toggleProvider = Mockito.mock(PolicyToggleProvider.class);
        given(toggleProvider.isPasswordPolicyEnabled()).willReturn(true);

        PasswordPolicyValidator validator = new PasswordPolicyValidator(props, toggleProvider);

        assertThatThrownBy(() -> validator.validate("weakpw"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
