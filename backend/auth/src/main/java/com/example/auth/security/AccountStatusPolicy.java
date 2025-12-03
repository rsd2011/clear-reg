package com.example.auth.security;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.config.AuthPolicyProperties;
import com.example.common.user.spi.UserAccountInfo;
import com.example.common.user.spi.UserAccountProvider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Services pass domain entities directly; lifecycle managed by JPA")
@Component
public class AccountStatusPolicy {

  private final AuthPolicyProperties properties;
  private final UserAccountProvider userAccountProvider;
  private final PasswordHistoryService passwordHistoryService;
  private final PolicyToggleProvider policyToggleProvider;

  public AccountStatusPolicy(
      AuthPolicyProperties properties,
      UserAccountProvider userAccountProvider,
      PasswordHistoryService passwordHistoryService,
      PolicyToggleProvider policyToggleProvider) {
    this.properties = properties;
    this.userAccountProvider = userAccountProvider;
    this.passwordHistoryService = passwordHistoryService;
    this.policyToggleProvider = policyToggleProvider;
  }

  public void ensureLoginAllowed(UserAccountInfo account) {
    if (!account.isActive()) {
      throw new InvalidCredentialsException();
    }
    if (policyToggleProvider.isAccountLockEnabled() && account.isLocked()) {
      throw new InvalidCredentialsException();
    }
    if (policyToggleProvider.isPasswordHistoryEnabled()
        && passwordHistoryService.isExpired(account)) {
      throw new InvalidCredentialsException();
    }
  }

  @Transactional
  public void onSuccessfulLogin(UserAccountInfo account) {
    if (!policyToggleProvider.isAccountLockEnabled()) {
      return;
    }
    if (account.getFailedLoginAttempts() > 0 || account.getLockedUntil() != null) {
      userAccountProvider.resetFailedAttempts(account.getUsername());
      userAccountProvider.lockUntil(account.getUsername(), null);
    }
  }

  @Transactional
  public void onFailedLogin(UserAccountInfo account) {
    if (!policyToggleProvider.isAccountLockEnabled()) {
      throw new InvalidCredentialsException();
    }
    userAccountProvider.incrementFailedAttempt(account.getUsername());
    // 실패 횟수를 다시 조회하여 잠금 여부 판단
    UserAccountInfo updated = userAccountProvider.getByUsernameOrThrow(account.getUsername());
    if (updated.getFailedLoginAttempts() >= properties.getMaxFailedAttempts()) {
      userAccountProvider.lockUntil(
          account.getUsername(),
          Instant.now().plusSeconds(properties.getLockoutSeconds()));
      userAccountProvider.resetFailedAttempts(account.getUsername());
    }
    throw new InvalidCredentialsException();
  }

  @Transactional
  public void deactivate(UserAccountInfo account) {
    userAccountProvider.deactivate(account.getUsername());
  }

  @Transactional
  public void activate(UserAccountInfo account) {
    userAccountProvider.activate(account.getUsername());
  }
}
