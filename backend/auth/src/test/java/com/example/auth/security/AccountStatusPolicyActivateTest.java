package com.example.auth.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.auth.config.AuthPolicyProperties;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountStatusPolicyActivateTest {

  private AccountStatusPolicy policy(UserAccountRepository repo) {
    AuthPolicyProperties props = new AuthPolicyProperties();
    PasswordHistoryService history = mock(PasswordHistoryService.class);
    PolicyToggleProvider toggles = mock(PolicyToggleProvider.class);
    org.mockito.BDDMockito.given(toggles.isAccountLockEnabled()).willReturn(true);
    org.mockito.BDDMockito.given(toggles.isPasswordHistoryEnabled()).willReturn(false);
    org.mockito.BDDMockito.given(toggles.isPasswordPolicyEnabled()).willReturn(true);
    org.mockito.BDDMockito.given(toggles.enabledLoginTypes()).willReturn(java.util.List.of());
    org.mockito.BDDMockito.given(repo.save(any())).willAnswer(inv -> inv.getArgument(0));
    return new AccountStatusPolicy(props, repo, history, toggles);
  }

  @Test
  @DisplayName("deactivate와 activate 호출 시 저장소에 변경을 반영한다")
  void deactivateAndActivateSaves() {
    UserAccountRepository repo = mock(UserAccountRepository.class);
    AccountStatusPolicy policy = policy(repo);
    UserAccount user =
        UserAccount.builder()
            .username("u")
            .password("p")
            .organizationCode("ORG")
            .permissionGroupCode("PG")
            .build();

    policy.deactivate(user);
    policy.activate(user);

    verify(repo, org.mockito.Mockito.atLeastOnce()).save(any());
  }
}
