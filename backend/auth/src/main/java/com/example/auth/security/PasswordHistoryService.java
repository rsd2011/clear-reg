package com.example.auth.security;

import com.example.admin.user.domain.UserAccount;
import com.example.admin.user.repository.UserAccountRepository;
import com.example.auth.InvalidCredentialsException;
import com.example.auth.config.AuthPolicyProperties;
import com.example.auth.domain.PasswordHistory;
import com.example.auth.domain.PasswordHistoryRepository;
import com.example.common.user.spi.UserAccountInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Service intentionally passes domain entities to repositories")
@Service
public class PasswordHistoryService {

  private final PasswordHistoryRepository repository;
  private final UserAccountRepository userAccountRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthPolicyProperties properties;
  private final PolicyToggleProvider policyToggleProvider;

  public PasswordHistoryService(
      PasswordHistoryRepository repository,
      UserAccountRepository userAccountRepository,
      PasswordEncoder passwordEncoder,
      AuthPolicyProperties properties,
      PolicyToggleProvider policyToggleProvider) {
    this.repository = repository;
    this.userAccountRepository = userAccountRepository;
    this.passwordEncoder = passwordEncoder;
    this.properties = properties;
    this.policyToggleProvider = policyToggleProvider;
  }

  @Transactional
  public void record(String username, String encodedPassword) {
    if (!policyToggleProvider.isPasswordHistoryEnabled()) {
      return;
    }
    UserAccount user = userAccountRepository.findByUsername(username)
        .orElseThrow(() -> new InvalidCredentialsException());
    PasswordHistory history = new PasswordHistory(user, encodedPassword);
    repository.save(history);
    pruneHistory(username);
  }

  public void ensureNotReused(String username, String rawPassword) {
    if (!policyToggleProvider.isPasswordHistoryEnabled()) {
      return;
    }
    List<PasswordHistory> histories = repository.findByUserUsernameOrderByChangedAtDesc(username);
    histories.stream()
        .limit(properties.getPasswordHistorySize())
        .filter(history -> passwordEncoder.matches(rawPassword, history.getPasswordHash()))
        .findFirst()
        .ifPresent(
            history -> {
              throw new InvalidCredentialsException();
            });
  }

  private void pruneHistory(String username) {
    List<PasswordHistory> histories = repository.findByUserUsernameOrderByChangedAtDesc(username);
    int max = properties.getPasswordHistorySize();
    for (int i = max; i < histories.size(); i++) {
      repository.delete(histories.get(i));
    }
  }

  public boolean isExpired(UserAccountInfo account) {
    if (!policyToggleProvider.isPasswordHistoryEnabled()) {
      return false;
    }
    if (properties.getPasswordExpiryDays() <= 0) {
      return false;
    }
    Instant changedAt = account.getPasswordChangedAt();
    if (changedAt == null) {
      return true;
    }
    Instant expiry = changedAt.plusSeconds(properties.getPasswordExpiryDays() * 86400L);
    return Instant.now().isAfter(expiry);
  }
}
