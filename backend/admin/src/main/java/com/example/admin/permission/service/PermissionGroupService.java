package com.example.admin.permission.service;

import com.example.admin.permission.domain.PermissionGroup;
import com.example.admin.permission.event.PermissionSetChangedEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.example.admin.permission.repository.PermissionGroupRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
public class PermissionGroupService implements ApplicationEventPublisherAware {

  private static final Duration CACHE_TTL = Duration.ofMinutes(5);

  private final PermissionGroupRepository repository;
  private final Map<String, CachedPermissionGroup> cache = new ConcurrentHashMap<>();
  private ApplicationEventPublisher eventPublisher;

  public PermissionGroupService(PermissionGroupRepository repository) {
    this.repository = repository;
  }

  @Override
  public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
    this.eventPublisher = applicationEventPublisher;
  }

  public PermissionGroup getByCodeOrThrow(String code) {
    return Optional.ofNullable(cache.get(code))
        .filter(entry -> !entry.isExpired())
        .map(CachedPermissionGroup::group)
        .orElseGet(
            () ->
                repository
                    .findByCode(code)
                    .map(
                        group -> {
                          cache.put(code, new CachedPermissionGroup(group));
                          return group;
                        })
                    .orElseThrow(() -> new IllegalArgumentException("권한그룹을 찾을 수 없습니다: " + code)));
  }

  public void evict(String code) {
    cache.remove(code);
    publishChange(null);
  }

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
