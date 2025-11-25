package com.example.auth.security;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.config.AuthPolicyProperties;
import com.example.auth.domain.PasswordHistory;
import com.example.auth.domain.PasswordHistoryRepository;
import com.example.auth.domain.UserAccount;
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
  private final PasswordEncoder passwordEncoder;
  private final AuthPolicyProperties properties;
  private final PolicyToggleProvider policyToggleProvider;

  public PasswordHistoryService(
      PasswordHistoryRepository repository,
      PasswordEncoder passwordEncoder,
      AuthPolicyProperties properties,
      PolicyToggleProvider policyToggleProvider) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
    this.properties = properties;
    this.policyToggleProvider = policyToggleProvider;
  }

  @Transactional
  public void record(UserAccount user, String encodedPassword) {
    if (!policyToggleProvider.isPasswordHistoryEnabled()) {
      return;
    }
    PasswordHistory history = new PasswordHistory(user, encodedPassword);
    repository.save(history);
    pruneHistory(user);
  }

  public void ensureNotReused(UserAccount user, String rawPassword) {
    if (!policyToggleProvider.isPasswordHistoryEnabled()) {
      return;
    }
    List<PasswordHistory> histories = repository.findByUserOrderByChangedAtDesc(user);
    histories.stream()
        .limit(properties.getPasswordHistorySize())
        .filter(history -> passwordEncoder.matches(rawPassword, history.getPasswordHash()))
        .findFirst()
        .ifPresent(
            history -> {
              throw new InvalidCredentialsException();
            });
  }

  private void pruneHistory(UserAccount user) {
    List<PasswordHistory> histories = repository.findByUserOrderByChangedAtDesc(user);
    int max = properties.getPasswordHistorySize();
    for (int i = max; i < histories.size(); i++) {
      repository.delete(histories.get(i));
    }
  }

  public boolean isExpired(UserAccount user) {
    if (!policyToggleProvider.isPasswordHistoryEnabled()) {
      return false;
    }
    if (properties.getPasswordExpiryDays() <= 0) {
      return false;
    }
    Instant changedAt = user.getPasswordChangedAt();
    if (changedAt == null) {
      return true;
    }
    Instant expiry = changedAt.plusSeconds(properties.getPasswordExpiryDays() * 86400L);
    return Instant.now().isAfter(expiry);
  }
}
