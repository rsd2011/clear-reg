package com.example.auth.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.config.AuthPolicyProperties;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountRepository;

class AccountStatusPolicyNoLockTest {

    @Test
    @DisplayName("락 토글이 꺼져 있으면 onFailedLogin은 바로 InvalidCredentialsException을 던진다")
    void onFailedLoginWhenLockDisabled() {
        AuthPolicyProperties props = new AuthPolicyProperties();
        UserAccountRepository repo = org.mockito.Mockito.mock(UserAccountRepository.class);
        PasswordHistoryService history = org.mockito.Mockito.mock(PasswordHistoryService.class);
        PolicyToggleProvider toggles = org.mockito.Mockito.mock(PolicyToggleProvider.class);
        given(toggles.isAccountLockEnabled()).willReturn(false);
        given(toggles.isPasswordHistoryEnabled()).willReturn(false);
        given(toggles.isPasswordPolicyEnabled()).willReturn(true);
        given(toggles.enabledLoginTypes()).willReturn(java.util.List.of());

        AccountStatusPolicy policy = new AccountStatusPolicy(props, repo, history, toggles);
        UserAccount user = UserAccount.builder().username("u").password("p").organizationCode("ORG").permissionGroupCode("PG").build();

        assertThatThrownBy(() -> policy.onFailedLogin(user)).isInstanceOf(InvalidCredentialsException.class);
        verify(repo, never()).save(any());
    }
}
