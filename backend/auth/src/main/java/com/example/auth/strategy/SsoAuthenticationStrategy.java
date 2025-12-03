package com.example.auth.strategy;

import com.example.admin.user.service.JitProvisioningService;
import com.example.auth.InvalidCredentialsException;
import com.example.auth.LoginType;
import com.example.auth.dto.LoginRequest;
import com.example.auth.sso.SsoClient;
import com.example.common.user.spi.UserAccountInfo;
import com.example.common.user.spi.UserAccountProvider;
import org.springframework.stereotype.Component;

@Component
public class SsoAuthenticationStrategy implements AuthenticationStrategy {

  private final SsoClient ssoClient;
  private final UserAccountProvider userAccountProvider;
  private final JitProvisioningService jitProvisioningService;

  public SsoAuthenticationStrategy(
      SsoClient ssoClient,
      UserAccountProvider userAccountProvider,
      JitProvisioningService jitProvisioningService) {
    this.ssoClient = ssoClient;
    this.userAccountProvider = userAccountProvider;
    this.jitProvisioningService = jitProvisioningService;
  }

  @Override
  public LoginType supportedType() {
    return LoginType.SSO;
  }

  @Override
  public UserAccountInfo authenticate(LoginRequest request) {
    if (request.token() == null) {
      throw new InvalidCredentialsException();
    }
    String username = ssoClient.resolveUsername(request.token());
    String ssoId = ssoClient.resolveSsoId(request.token());

    // JIT Provisioning이 활성화된 경우 자동 생성/연결 수행
    if (jitProvisioningService.isEnabledFor(LoginType.SSO.name())) {
      return jitProvisioningService.provisionForSso(ssoId, username).account();
    }

    return userAccountProvider.getByUsernameOrThrow(username);
  }
}
