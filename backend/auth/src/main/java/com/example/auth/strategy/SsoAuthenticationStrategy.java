package com.example.auth.strategy;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.LoginType;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountService;
import com.example.auth.dto.LoginRequest;
import com.example.auth.jit.JitProvisioningService;
import com.example.auth.sso.SsoClient;
import org.springframework.stereotype.Component;

@Component
public class SsoAuthenticationStrategy implements AuthenticationStrategy {

  private final SsoClient ssoClient;
  private final UserAccountService userAccountService;
  private final JitProvisioningService jitProvisioningService;

  public SsoAuthenticationStrategy(
      SsoClient ssoClient,
      UserAccountService userAccountService,
      JitProvisioningService jitProvisioningService) {
    this.ssoClient = ssoClient;
    this.userAccountService = userAccountService;
    this.jitProvisioningService = jitProvisioningService;
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
    String ssoId = ssoClient.resolveSsoId(request.token());

    // JIT Provisioning이 활성화된 경우 자동 생성/연결 수행
    if (jitProvisioningService.isEnabledFor(LoginType.SSO)) {
      return jitProvisioningService.provisionForSso(ssoId, username).account();
    }

    return userAccountService.getByUsernameOrThrow(username);
  }
}
