package com.example.auth.permission;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class PermissionGroupService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final PermissionGroupRepository repository;
    private final Map<String, CachedPermissionGroup> cache = new ConcurrentHashMap<>();

    public PermissionGroupService(PermissionGroupRepository repository) {
        this.repository = repository;
    }

    public PermissionGroup getByCodeOrThrow(String code) {
        return Optional.ofNullable(cache.get(code))
                .filter(entry -> !entry.isExpired())
                .map(CachedPermissionGroup::group)
                .orElseGet(() -> repository.findByCode(code)
                        .map(group -> {
                            cache.put(code, new CachedPermissionGroup(group));
                            return group;
                        })
                        .orElseThrow(() -> new IllegalArgumentException("권한그룹을 찾을 수 없습니다: " + code)));
    }

    public void evict(String code) {
        cache.remove(code);
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
