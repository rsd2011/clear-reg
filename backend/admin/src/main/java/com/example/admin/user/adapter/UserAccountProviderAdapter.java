package com.example.admin.user.adapter;

import com.example.admin.user.domain.UserAccount;
import com.example.admin.user.service.UserAccountService;
import com.example.common.user.spi.UserAccountInfo;
import com.example.common.user.spi.UserAccountProvider;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * UserAccountProvider SPI 구현체.
 *
 * <p>Platform 모듈에서 정의된 SPI를 구현하여 Auth 모듈에서 사용자 정보에 접근할 수 있게 합니다.
 */
@Component
public class UserAccountProviderAdapter implements UserAccountProvider {

  private final UserAccountService userAccountService;

  public UserAccountProviderAdapter(UserAccountService userAccountService) {
    this.userAccountService = userAccountService;
  }

  @Override
  public UserAccount getByUsernameOrThrow(String username) {
    return userAccountService.getByUsernameOrThrow(username);
  }

  @Override
  public Optional<UserAccountInfo> findByUsername(String username) {
    return userAccountService.findByUsername(username)
        .map(account -> account);
  }

  @Override
  public Optional<UserAccountInfo> findBySsoId(String ssoId) {
    return userAccountService.findBySsoId(ssoId)
        .map(account -> account);
  }

  @Override
  public List<? extends UserAccountInfo> findByPermissionGroupCodeIn(List<String> codes) {
    return userAccountService.findByPermissionGroupCodeIn(codes);
  }

  @Override
  public boolean passwordMatches(String username, String rawPassword) {
    UserAccount account = userAccountService.getByUsernameOrThrow(username);
    return userAccountService.passwordMatches(account, rawPassword);
  }

  @Override
  public void incrementFailedAttempt(String username) {
    userAccountService.incrementFailedAttempt(username);
  }

  @Override
  public void resetFailedAttempts(String username) {
    userAccountService.resetFailedAttempts(username);
  }

  @Override
  public void lockUntil(String username, Instant until) {
    userAccountService.lockUntil(username, until);
  }

  @Override
  public void activate(String username) {
    userAccountService.activate(username);
  }

  @Override
  public void deactivate(String username) {
    userAccountService.deactivate(username);
  }

  @Override
  public void updatePassword(String username, String encodedPassword) {
    userAccountService.updatePassword(username, encodedPassword);
  }
}
