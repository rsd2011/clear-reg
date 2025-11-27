package com.example.auth.strategy;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.LoginType;
import com.example.auth.ad.ActiveDirectoryClient;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountService;
import com.example.auth.dto.LoginRequest;
import com.example.auth.jit.JitProvisioningService;
import org.springframework.stereotype.Component;

@Component
public class ActiveDirectoryAuthenticationStrategy implements AuthenticationStrategy {

  private final ActiveDirectoryClient activeDirectoryClient;
  private final UserAccountService userAccountService;
  private final JitProvisioningService jitProvisioningService;

  public ActiveDirectoryAuthenticationStrategy(
      ActiveDirectoryClient activeDirectoryClient,
      UserAccountService userAccountService,
      JitProvisioningService jitProvisioningService) {
    this.activeDirectoryClient = activeDirectoryClient;
    this.userAccountService = userAccountService;
    this.jitProvisioningService = jitProvisioningService;
  }

  @Override
  public LoginType supportedType() {
    return LoginType.AD;
  }

  @Override
  public UserAccount authenticate(LoginRequest request) {
    if (request.username() == null || request.password() == null) {
      throw new InvalidCredentialsException();
    }
    if (!activeDirectoryClient.authenticate(request.username(), request.password())) {
      throw new InvalidCredentialsException();
    }

    // JIT Provisioning이 활성화된 경우 자동 생성/연결 수행
    if (jitProvisioningService.isEnabledFor(LoginType.AD)) {
      String adDomain = activeDirectoryClient.getDomain();
      return jitProvisioningService.provisionForAd(request.username(), adDomain).account();
    }

    return userAccountService.getByUsernameOrThrow(request.username());
  }
}
