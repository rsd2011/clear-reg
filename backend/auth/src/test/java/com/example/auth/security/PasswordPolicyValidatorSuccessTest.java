package com.example.auth.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.auth.config.AuthPolicyProperties;

class PasswordPolicyValidatorSuccessTest {

    @Test
    @DisplayName("정책을 모두 충족하면 예외 없이 통과한다")
    void validPasswordPasses() {
        AuthPolicyProperties props = new AuthPolicyProperties();
        props.setPasswordMinLength(8);
        props.setRequireUppercase(true);
        props.setRequireLowercase(true);
        props.setRequireDigit(true);
        props.setRequireSpecial(true);

        PolicyToggleProvider toggle = new PolicyToggleProvider() {
            @Override public boolean isPasswordPolicyEnabled() { return true; }
            @Override public boolean isPasswordHistoryEnabled() { return false; }
            @Override public boolean isAccountLockEnabled() { return false; }
            @Override public java.util.List<com.example.auth.LoginType> enabledLoginTypes() { return java.util.List.of(); }
        };
        PasswordPolicyValidator validator = new PasswordPolicyValidator(props, toggle);

        validator.validate("Aa1!good");
    }
}
