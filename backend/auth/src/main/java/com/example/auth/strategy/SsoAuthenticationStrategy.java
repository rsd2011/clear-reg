package com.example.auth.strategy;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.LoginType;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountService;
import com.example.auth.dto.LoginRequest;
import com.example.auth.sso.SsoClient;
import org.springframework.stereotype.Component;

@Component
public class SsoAuthenticationStrategy implements AuthenticationStrategy {

  private final SsoClient ssoClient;
  private final UserAccountService userAccountService;

  public SsoAuthenticationStrategy(SsoClient ssoClient, UserAccountService userAccountService) {
    this.ssoClient = ssoClient;
    this.userAccountService = userAccountService;
  }

  @Override
  public LoginType supportedType() {
    return LoginType.SSO;
  }

  @Override
  public UserAccount authenticate(LoginRequest request) {
    if (request.token() == null) {
      throw new InvalidCredentialsException();
    }
    String username = ssoClient.resolveUsername(request.token());
    return userAccountService.getByUsernameOrThrow(username);
  }
}
