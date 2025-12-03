package com.example.admin.user.service;

import com.example.admin.user.domain.UserAccount;
import com.example.admin.user.exception.UserNotFoundException;
import com.example.admin.user.repository.UserAccountRepository;
import com.example.common.cache.CacheNames;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 계정 서비스.
 *
 * <p>사용자 조회, 저장, 계정 상태 관리를 담당합니다.
 * 캐시는 사용자명을 키로 사용합니다.
 */
@Service
@CacheConfig(cacheNames = CacheNames.USER_ACCOUNTS)
public class UserAccountService {

  private final UserAccountRepository repository;
  private final PasswordEncoder passwordEncoder;

  public UserAccountService(
      UserAccountRepository repository,
      PasswordEncoder passwordEncoder) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * 사용자명으로 사용자를 조회합니다. 캐시됩니다.
   *
   * @param username 사용자명
   * @return 사용자 계정
   * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
   */
  @Cacheable(key = "#root.args[0]", sync = true)
  public UserAccount getByUsernameOrThrow(String username) {
    return repository.findByUsername(username)
        .orElseThrow(() -> new UserNotFoundException(username));
  }

  /**
   * 사용자명으로 사용자를 조회합니다.
   *
   * @param username 사용자명
   * @return 사용자 계정 (Optional)
   */
  public Optional<UserAccount> findByUsername(String username) {
    return repository.findByUsername(username);
  }

  /**
   * SSO ID로 사용자를 조회합니다. 캐시되지 않습니다.
   *
   * @param ssoId SSO ID
   * @return 사용자 계정 (Optional)
   */
  public Optional<UserAccount> findBySsoId(String ssoId) {
    return repository.findBySsoId(ssoId);
  }

  /**
   * 권한 그룹 코드 목록에 해당하는 사용자들을 조회합니다.
   *
   * @param codes 권한 그룹 코드 목록
   * @return 사용자 목록
   */
  public List<UserAccount> findByPermissionGroupCodeIn(List<String> codes) {
    return repository.findByPermissionGroupCodeIn(codes);
  }

  /**
   * 사용자를 저장합니다. 캐시가 갱신됩니다.
   *
   * @param account 저장할 사용자
   * @return 저장된 사용자
   */
  @Transactional
  @CachePut(key = "#result.username", condition = "#result != null && #result.username != null")
  public UserAccount save(UserAccount account) {
    return repository.save(account);
  }

  /**
   * 비밀번호가 일치하는지 확인합니다.
   *
   * @param account 사용자 계정
   * @param rawPassword 평문 비밀번호
   * @return 일치 여부
   */
  public boolean passwordMatches(UserAccount account, String rawPassword) {
    return passwordEncoder.matches(rawPassword, account.getPassword());
  }

  /**
   * 비밀번호를 변경합니다. 캐시가 무효화됩니다.
   *
   * @param username 사용자명
   * @param encodedPassword 암호화된 새 비밀번호
   */
  @Transactional
  @CacheEvict(key = "#root.args[0]")
  public void updatePassword(String username, String encodedPassword) {
    UserAccount account = getByUsernameOrThrow(username);
    account.updatePassword(encodedPassword);
    repository.save(account);
  }

  /**
   * 로그인 실패 횟수를 증가시킵니다. 캐시가 무효화됩니다.
   *
   * @param username 사용자명
   */
  @Transactional
  @CacheEvict(key = "#root.args[0]")
  public void incrementFailedAttempt(String username) {
    UserAccount account = getByUsernameOrThrow(username);
    account.incrementFailedAttempt();
    repository.save(account);
  }

  /**
   * 로그인 실패 횟수를 초기화합니다. 캐시가 무효화됩니다.
   *
   * @param username 사용자명
   */
  @Transactional
  @CacheEvict(key = "#root.args[0]")
  public void resetFailedAttempts(String username) {
    UserAccount account = getByUsernameOrThrow(username);
    account.resetFailedAttempts();
    repository.save(account);
  }

  /**
   * 계정을 특정 시간까지 잠급니다. 캐시가 무효화됩니다.
   *
   * @param username 사용자명
   * @param until 잠금 해제 시간
   */
  @Transactional
  @CacheEvict(key = "#root.args[0]")
  public void lockUntil(String username, Instant until) {
    UserAccount account = getByUsernameOrThrow(username);
    account.lockUntil(until);
    repository.save(account);
  }

  /**
   * 계정을 활성화합니다. 캐시가 무효화됩니다.
   *
   * @param username 사용자명
   */
  @Transactional
  @CacheEvict(key = "#root.args[0]")
  public void activate(String username) {
    UserAccount account = getByUsernameOrThrow(username);
    account.activate();
    repository.save(account);
  }

  /**
   * 계정을 비활성화합니다. 캐시가 무효화됩니다.
   *
   * @param username 사용자명
   */
  @Transactional
  @CacheEvict(key = "#root.args[0]")
  public void deactivate(String username) {
    UserAccount account = getByUsernameOrThrow(username);
    account.deactivate();
    repository.save(account);
  }

  /**
   * ID로 사용자를 조회합니다.
   *
   * @param id 사용자 ID
   * @return 사용자 계정
   * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
   */
  public UserAccount getByIdOrThrow(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
  }

  /**
   * ID로 사용자를 조회합니다.
   *
   * @param id 사용자 ID
   * @return 사용자 계정 (Optional)
   */
  public Optional<UserAccount> findById(UUID id) {
    return repository.findById(id);
  }

  /**
   * 사용자 목록을 페이징하여 조회합니다.
   *
   * @param specification 검색 조건
   * @param pageable 페이징 정보
   * @return 사용자 목록
   */
  public Page<UserAccount> findAll(Specification<UserAccount> specification, Pageable pageable) {
    return repository.findAll(specification, pageable);
  }

  /**
   * 사용자명 중복 여부를 확인합니다.
   *
   * @param username 사용자명
   * @return 중복 여부
   */
  public boolean existsByUsername(String username) {
    return repository.findByUsername(username).isPresent();
  }

  /**
   * 비밀번호를 인코딩합니다.
   *
   * @param rawPassword 평문 비밀번호
   * @return 인코딩된 비밀번호
   */
  public String encodePassword(String rawPassword) {
    return passwordEncoder.encode(rawPassword);
  }

  /**
   * 계정 잠금을 해제합니다. 캐시가 무효화됩니다.
   *
   * @param username 사용자명
   */
  @Transactional
  @CacheEvict(key = "#root.args[0]")
  public void unlock(String username) {
    UserAccount account = getByUsernameOrThrow(username);
    account.unlock();
    repository.save(account);
  }

  /**
   * 마지막 로그인 시간을 업데이트합니다. 캐시가 무효화됩니다.
   *
   * @param username 사용자명
   */
  @Transactional
  @CacheEvict(key = "#root.args[0]")
  public void updateLastLoginAt(String username) {
    UserAccount account = getByUsernameOrThrow(username);
    account.updateLastLoginAt(Instant.now());
    repository.save(account);
  }

  /**
   * 비밀번호를 초기화합니다. 캐시가 무효화됩니다.
   *
   * @param username 사용자명
   * @param newEncodedPassword 인코딩된 새 비밀번호
   */
  @Transactional
  @CacheEvict(key = "#root.args[0]")
  public void resetPassword(String username, String newEncodedPassword) {
    UserAccount account = getByUsernameOrThrow(username);
    account.updatePassword(newEncodedPassword);
    repository.save(account);
  }

  /**
   * 사용자를 삭제(비활성화)합니다. Soft delete로 처리됩니다.
   * 캐시가 무효화됩니다.
   *
   * @param id 사용자 ID
   */
  @Transactional
  public void softDelete(UUID id) {
    UserAccount account = getByIdOrThrow(id);
    account.deactivate();
    repository.save(account);
  }
}
