package com.example.auth.domain;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.security.PasswordHistoryService;
import com.example.common.cache.CacheNames;
import java.util.Optional;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@CacheConfig(cacheNames = CacheNames.USER_ACCOUNTS)
public class UserAccountService {

  private final UserAccountRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final PasswordHistoryService passwordHistoryService;

  public UserAccountService(
      UserAccountRepository repository,
      PasswordEncoder passwordEncoder,
      PasswordHistoryService passwordHistoryService) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
    this.passwordHistoryService = passwordHistoryService;
  }

  @Cacheable(key = "#root.args[0]", sync = true)
  public UserAccount getByUsernameOrThrow(String username) {
    return repository.findByUsername(username).orElseThrow(InvalidCredentialsException::new);
  }

  public Optional<UserAccount> findBySsoId(String ssoId) {
    return repository.findBySsoId(ssoId);
  }

  @Transactional
  @CachePut(key = "#result.username", condition = "#result != null && #result.username != null")
  public UserAccount save(UserAccount account) {
    return repository.save(account);
  }

  @Transactional
  @CacheEvict(key = "#root.args[0].username")
  public void changePassword(UserAccount account, String rawPassword) {
    passwordHistoryService.ensureNotReused(account, rawPassword);
    String encoded = passwordEncoder.encode(rawPassword);
    account.updatePassword(encoded);
    repository.save(account);
    passwordHistoryService.record(account, encoded);
  }

  public boolean passwordMatches(UserAccount account, String rawPassword) {
    return passwordEncoder.matches(rawPassword, account.getPassword());
  }
}
