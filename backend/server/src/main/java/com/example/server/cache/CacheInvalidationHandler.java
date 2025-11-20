package com.example.server.cache;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.example.common.cache.CacheInvalidationEvent;
import com.example.common.cache.CacheInvalidationType;
import com.example.common.cache.CacheNames;
import com.example.dw.application.readmodel.OrganizationReadModelPort;
import com.example.dw.application.readmodel.MenuReadModelPort;

@Component
public class CacheInvalidationHandler {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationHandler.class);

    private final CacheManager cacheManager;
    @Nullable
    private final OrganizationReadModelPort organizationReadModelPort;
    @Nullable
    private final MenuReadModelPort menuReadModelPort;

    public CacheInvalidationHandler(CacheManager cacheManager,
                                    @Nullable OrganizationReadModelPort organizationReadModelPort,
                                    @Nullable MenuReadModelPort menuReadModelPort) {
        this.cacheManager = cacheManager;
        this.organizationReadModelPort = organizationReadModelPort;
        this.menuReadModelPort = menuReadModelPort;
    }

    public void handle(CacheInvalidationEvent event) {
        if (event == null) {
            return;
        }
        CacheInvalidationType type = event.type();
        log.info("Received cache invalidation event: type={}, tenant={}, scope={}", type, event.tenantId(), event.scopeId());
        switch (type) {
            case ROW_SCOPE -> evict(CacheNames.ORGANIZATION_ROW_SCOPE);
            case ORGANIZATION -> {
                evict(CacheNames.DW_ORG_TREE);
                if (organizationReadModelPort != null && organizationReadModelPort.isEnabled()) {
                    organizationReadModelPort.evict();
                    organizationReadModelPort.rebuild();
                }
            }
            case PERMISSION_MENU -> {
                evict(CacheNames.USER_DETAILS);
                if (menuReadModelPort != null && menuReadModelPort.isEnabled()) {
                    menuReadModelPort.evict();
                    menuReadModelPort.rebuild();
                }
            }
            case MASKING -> evict(CacheNames.COMMON_CODE_AGGREGATES);
            default -> log.debug("Unhandled cache invalidation type {}", type);
        }
    }

    private void evict(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (Objects.nonNull(cache)) {
            cache.clear();
        }
    }
}
