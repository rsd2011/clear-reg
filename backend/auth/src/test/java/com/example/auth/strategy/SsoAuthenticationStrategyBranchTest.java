package com.example.auth.strategy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.LoginType;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountService;
import com.example.auth.dto.LoginRequest;
import com.example.auth.jit.JitProvisioningService;
import com.example.auth.sso.SsoClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SsoAuthenticationStrategyBranchTest {

  @Test
  @DisplayName("토큰이 없으면 InvalidCredentialsException")
  void nullTokenThrows() {
    SsoAuthenticationStrategy strategy =
        new SsoAuthenticationStrategy(
            mock(SsoClient.class),
            mock(UserAccountService.class),
            mock(JitProvisioningService.class));

    assertThatThrownBy(
            () -> strategy.authenticate(new LoginRequest(LoginType.SSO, null, null, null)))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  @DisplayName("resolveUsername 결과로 계정을 조회한다")
  void resolvesAndFetchesUser() {
    SsoClient sso = mock(SsoClient.class);
    UserAccountService userService = mock(UserAccountService.class);
    JitProvisioningService jitService = mock(JitProvisioningService.class);
    given(jitService.isEnabledFor(LoginType.SSO)).willReturn(false);
    given(sso.resolveUsername("token")).willReturn("alice");
    given(sso.resolveSsoId("token")).willReturn("token");
    given(userService.getByUsernameOrThrow("alice"))
        .willReturn(
            UserAccount.builder()
                .username("alice")
                .password("p")
                .organizationCode("ORG")
                .permissionGroupCode("PG")
                .build());

    SsoAuthenticationStrategy strategy = new SsoAuthenticationStrategy(sso, userService, jitService);
    strategy.authenticate(new LoginRequest(LoginType.SSO, null, null, "token"));

    verify(sso).resolveUsername("token");
    verify(userService).getByUsernameOrThrow("alice");
  }
}
