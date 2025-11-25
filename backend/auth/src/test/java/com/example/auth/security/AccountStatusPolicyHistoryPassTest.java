package com.example.auth.security;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.example.auth.config.AuthPolicyProperties;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountStatusPolicyHistoryPassTest {

  @Test
  @DisplayName("비밀번호 이력 만료 체크가 활성이어도 isExpired=false면 통과한다")
  void historyEnabledButNotExpiredPasses() {
    AuthPolicyProperties props = new AuthPolicyProperties();
    UserAccountRepository repo = mock(UserAccountRepository.class);
    PasswordHistoryService history = mock(PasswordHistoryService.class);
    PolicyToggleProvider toggles = mock(PolicyToggleProvider.class);
    given(toggles.isAccountLockEnabled()).willReturn(true);
    given(toggles.isPasswordHistoryEnabled()).willReturn(true);
    given(toggles.isPasswordPolicyEnabled()).willReturn(true);
    given(toggles.enabledLoginTypes()).willReturn(java.util.List.of());
    given(history.isExpired(org.mockito.ArgumentMatchers.any())).willReturn(false);

    AccountStatusPolicy policy = new AccountStatusPolicy(props, repo, history, toggles);
    UserAccount user =
        UserAccount.builder()
            .username("u")
            .password("p")
            .organizationCode("ORG")
            .permissionGroupCode("PG")
            .build();

    policy.ensureLoginAllowed(user); // should pass without exception
  }
}
