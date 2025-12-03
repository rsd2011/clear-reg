package com.example.auth.strategy;

import com.example.admin.user.service.JitProvisioningService;
import com.example.auth.InvalidCredentialsException;
import com.example.auth.LoginType;
import com.example.auth.ad.ActiveDirectoryClient;
import com.example.auth.dto.LoginRequest;
import com.example.common.user.spi.UserAccountInfo;
import com.example.common.user.spi.UserAccountProvider;
import org.springframework.stereotype.Component;

@Component
public class ActiveDirectoryAuthenticationStrategy implements AuthenticationStrategy {

  private final ActiveDirectoryClient activeDirectoryClient;
  private final UserAccountProvider userAccountProvider;
  private final JitProvisioningService jitProvisioningService;

  public ActiveDirectoryAuthenticationStrategy(
      ActiveDirectoryClient activeDirectoryClient,
      UserAccountProvider userAccountProvider,
      JitProvisioningService jitProvisioningService) {
    this.activeDirectoryClient = activeDirectoryClient;
    this.userAccountProvider = userAccountProvider;
    this.jitProvisioningService = jitProvisioningService;
  }

  @Override
  public LoginType supportedType() {
    return LoginType.AD;
  }

  @Override
  public UserAccountInfo authenticate(LoginRequest request) {
    if (request.username() == null || request.password() == null) {
      throw new InvalidCredentialsException();
    }
    if (!activeDirectoryClient.authenticate(request.username(), request.password())) {
      throw new InvalidCredentialsException();
    }

    // JIT Provisioning이 활성화된 경우 자동 생성/연결 수행
    if (jitProvisioningService.isEnabledFor(LoginType.AD.name())) {
      String adDomain = activeDirectoryClient.getDomain();
      return jitProvisioningService.provisionForAd(request.username(), adDomain).account();
    }

    return userAccountProvider.getByUsernameOrThrow(request.username());
  }
}
