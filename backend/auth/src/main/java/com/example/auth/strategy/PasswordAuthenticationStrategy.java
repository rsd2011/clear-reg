package com.example.auth.strategy;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.LoginType;
import com.example.auth.dto.LoginRequest;
import com.example.auth.security.AccountStatusPolicy;
import com.example.auth.security.PasswordPolicyValidator;
import com.example.common.user.spi.UserAccountInfo;
import com.example.common.user.spi.UserAccountProvider;
import org.springframework.stereotype.Component;

@Component
public class PasswordAuthenticationStrategy implements AuthenticationStrategy {

  private final UserAccountProvider userAccountProvider;
  private final PasswordPolicyValidator passwordPolicyValidator;
  private final AccountStatusPolicy accountStatusPolicy;

  public PasswordAuthenticationStrategy(
      UserAccountProvider userAccountProvider,
      PasswordPolicyValidator passwordPolicyValidator,
      AccountStatusPolicy accountStatusPolicy) {
    this.userAccountProvider = userAccountProvider;
    this.passwordPolicyValidator = passwordPolicyValidator;
    this.accountStatusPolicy = accountStatusPolicy;
  }

  @Override
  public LoginType supportedType() {
    return LoginType.PASSWORD;
  }

  @Override
  public UserAccountInfo authenticate(LoginRequest request) {
    if (request.username() == null || request.password() == null) {
      throw new InvalidCredentialsException();
    }
    passwordPolicyValidator.validate(request.password());
    UserAccountInfo account = userAccountProvider.getByUsernameOrThrow(request.username());
    accountStatusPolicy.ensureLoginAllowed(account);
    if (!userAccountProvider.passwordMatches(request.username(), request.password())) {
      accountStatusPolicy.onFailedLogin(account);
    }
    accountStatusPolicy.onSuccessfulLogin(account);
    return account;
  }
}
