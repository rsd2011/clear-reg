package com.example.admin.permission.service;

import com.example.admin.permission.domain.PermissionGroup;
import com.example.admin.permission.domain.PermissionGroupRoot;
import com.example.admin.permission.event.PermissionSetChangedEvent;
import com.example.admin.permission.repository.PermissionGroupRepository;
import com.example.admin.permission.repository.PermissionGroupRootRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * 권한 그룹 서비스.
 * <p>
 * 권한 그룹의 현재 활성 버전을 조회하고 캐싱합니다.
 * </p>
 */
@Service
public class PermissionGroupService implements ApplicationEventPublisherAware {

  private static final Duration CACHE_TTL = Duration.ofMinutes(5);

  private final PermissionGroupRootRepository rootRepository;
  private final PermissionGroupRepository versionRepository;
  private final Map<String, CachedPermissionGroup> cache = new ConcurrentHashMap<>();
  private ApplicationEventPublisher eventPublisher;

  public PermissionGroupService(PermissionGroupRootRepository rootRepository,
                                 PermissionGroupRepository versionRepository) {
    this.rootRepository = rootRepository;
    this.versionRepository = versionRepository;
  }

  @Override
  public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
    this.eventPublisher = applicationEventPublisher;
  }

  /**
   * 권한 그룹 코드로 현재 활성 버전을 조회합니다.
   *
   * @param code 권한 그룹 코드
   * @return 현재 활성 PermissionGroup 버전
   * @throws IllegalArgumentException 권한 그룹을 찾을 수 없을 때
   */
  public PermissionGroup getByCodeOrThrow(String code) {
    return Optional.ofNullable(cache.get(code))
        .filter(entry -> !entry.isExpired())
        .map(CachedPermissionGroup::group)
        .orElseGet(() -> {
          PermissionGroupRoot root = rootRepository.findByGroupCode(code)
              .orElseThrow(() -> new IllegalArgumentException("권한그룹을 찾을 수 없습니다: " + code));

          PermissionGroup currentVersion = root.getCurrentVersion();
          if (currentVersion == null) {
            throw new IllegalArgumentException("권한그룹의 활성 버전이 없습니다: " + code);
          }

          cache.put(code, new CachedPermissionGroup(currentVersion));
          return currentVersion;
        });
  }

  /**
   * 권한 그룹 코드로 루트 엔티티를 조회합니다.
   *
   * @param code 권한 그룹 코드
   * @return PermissionGroupRoot (Optional)
   */
  public Optional<PermissionGroupRoot> findRootByCode(String code) {
    return rootRepository.findByGroupCode(code);
  }

  /**
   * 권한 그룹 코드로 루트 엔티티를 조회하거나 예외를 발생시킵니다.
   *
   * @param code 권한 그룹 코드
   * @return PermissionGroupRoot
   * @throws IllegalArgumentException 권한 그룹을 찾을 수 없을 때
   */
  public PermissionGroupRoot getRootByCodeOrThrow(String code) {
    return rootRepository.findByGroupCode(code)
        .orElseThrow(() -> new IllegalArgumentException("권한그룹을 찾을 수 없습니다: " + code));
  }

  /**
   * 캐시를 무효화합니다.
   *
   * @param code 권한 그룹 코드
   */
  public void evict(String code) {
    cache.remove(code);
    publishChange(null);
  }

  /**
   * 모든 캐시를 무효화합니다.
   */
  public void evictAll() {
    cache.clear();
    publishChange(null);
  }

  /**
   * 권한 변경 이벤트를 발행합니다.
   *
   * @param principalId 변경 대상 사용자 ID (null이면 전체)
   */
  public void publishChange(@Nullable String principalId) {
    if (eventPublisher != null) {
      eventPublisher.publishEvent(new PermissionSetChangedEvent(principalId));
    }
  }

  private static final class CachedPermissionGroup {

    private final PermissionGroup group;
    private final Instant cachedAt;

    private CachedPermissionGroup(PermissionGroup group) {
      this.group = group;
      this.cachedAt = Instant.now();
    }

    private boolean isExpired() {
      return cachedAt.plus(CACHE_TTL).isBefore(Instant.now());
    }

    private PermissionGroup group() {
      return group;
    }
  }
}
