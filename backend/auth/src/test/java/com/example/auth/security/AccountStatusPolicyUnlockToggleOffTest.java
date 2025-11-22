package com.example.auth.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.auth.config.AuthPolicyProperties;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountRepository;

class AccountStatusPolicyUnlockToggleOffTest {

    @Test
    @DisplayName("isAccountLockEnabled가 false면 onSuccessfulLogin은 저장소를 호출하지 않는다")
    void onSuccessfulLoginNoOpWhenLockDisabled() {
        AuthPolicyProperties props = new AuthPolicyProperties();
        UserAccountRepository repo = mock(UserAccountRepository.class);
        PasswordHistoryService history = mock(PasswordHistoryService.class);
        PolicyToggleProvider toggles = mock(PolicyToggleProvider.class);
        org.mockito.BDDMockito.given(toggles.isAccountLockEnabled()).willReturn(false);
        org.mockito.BDDMockito.given(toggles.isPasswordHistoryEnabled()).willReturn(false);
        org.mockito.BDDMockito.given(toggles.isPasswordPolicyEnabled()).willReturn(true);
        org.mockito.BDDMockito.given(toggles.enabledLoginTypes()).willReturn(java.util.List.of());

        AccountStatusPolicy policy = new AccountStatusPolicy(props, repo, history, toggles);
        UserAccount user = UserAccount.builder().username("u").password("p").organizationCode("ORG").permissionGroupCode("PG").build();

        policy.onSuccessfulLogin(user);

        verify(repo, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
