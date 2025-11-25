package com.example.auth.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.LoginType;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountService;
import com.example.auth.dto.LoginRequest;
import com.example.auth.security.AccountStatusPolicy;
import com.example.auth.security.PasswordPolicyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordAuthenticationStrategy 테스트")
class PasswordAuthenticationStrategyTest {

  @Mock private UserAccountService userAccountService;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private PasswordPolicyValidator passwordPolicyValidator;

  @Mock private AccountStatusPolicy accountStatusPolicy;

  private PasswordAuthenticationStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy =
        new PasswordAuthenticationStrategy(
            userAccountService, passwordEncoder, passwordPolicyValidator, accountStatusPolicy);
  }

  @Test
  @DisplayName("Given 올바른 자격 When authenticate 호출 Then UserAccount를 반환한다")
  void givenValidCredentialsWhenAuthenticateThenReturnAccount() {
    var account =
        UserAccount.builder()
            .username("tester")
            .password("encoded")
            .email("tester@example.com")
            .build();
    given(userAccountService.getByUsernameOrThrow("tester")).willReturn(account);
    willDoNothing().given(accountStatusPolicy).ensureLoginAllowed(account);
    willDoNothing().given(accountStatusPolicy).onSuccessfulLogin(account);
    given(passwordEncoder.matches("pw", "encoded")).willReturn(true);

    UserAccount result =
        strategy.authenticate(new LoginRequest(LoginType.PASSWORD, "tester", "pw", null));

    assertThat(result).isEqualTo(account);
  }

  @Test
  @DisplayName("Given 잘못된 자격 When authenticate 호출 Then InvalidCredentialsException을 던진다")
  void givenInvalidCredentialsWhenAuthenticateThenThrow() {
    var account =
        UserAccount.builder()
            .username("tester")
            .password("encoded")
            .email("tester@example.com")
            .build();
    given(userAccountService.getByUsernameOrThrow("tester")).willReturn(account);
    given(passwordEncoder.matches("wrong", "encoded")).willReturn(false);
    willDoNothing().given(accountStatusPolicy).ensureLoginAllowed(account);
    willThrow(new InvalidCredentialsException()).given(accountStatusPolicy).onFailedLogin(account);

    assertThatThrownBy(
            () ->
                strategy.authenticate(
                    new LoginRequest(LoginType.PASSWORD, "tester", "wrong", null)))
        .isInstanceOf(InvalidCredentialsException.class);
  }
}
