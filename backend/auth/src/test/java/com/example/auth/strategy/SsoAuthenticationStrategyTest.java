package com.example.auth.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.example.auth.LoginType;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountService;
import com.example.auth.dto.LoginRequest;
import com.example.auth.sso.SsoClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SsoAuthenticationStrategy 테스트")
class SsoAuthenticationStrategyTest {

  @Mock private SsoClient ssoClient;

  @Mock private UserAccountService userAccountService;

  private SsoAuthenticationStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy = new SsoAuthenticationStrategy(ssoClient, userAccountService);
  }

  @Test
  @DisplayName("Given SSO 토큰 When authenticate 호출 Then 사용자 이름을 해석한다")
  void givenTokenWhenAuthenticateThenResolveUsername() {
    var account =
        UserAccount.builder()
            .username("test-user")
            .password("ignored")
            .email("user@example.com")
            .build();
    given(ssoClient.resolveUsername("SSO-test-user")).willReturn("test-user");
    given(userAccountService.getByUsernameOrThrow("test-user")).willReturn(account);

    UserAccount result =
        strategy.authenticate(new LoginRequest(LoginType.SSO, null, null, "SSO-test-user"));

    assertThat(result).isEqualTo(account);
  }
}
