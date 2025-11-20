package com.example.server.cache;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import com.example.common.cache.CacheInvalidationEvent;
import com.example.common.cache.CacheInvalidationType;
import com.example.common.cache.CacheNames;
import com.example.dw.application.readmodel.OrganizationReadModelPort;

class CacheInvalidationHandlerTest {

    private final CacheManager cacheManager = Mockito.mock(CacheManager.class);
    private final OrganizationReadModelPort readModelPort = Mockito.mock(OrganizationReadModelPort.class);
    private final CacheInvalidationHandler handler = new CacheInvalidationHandler(cacheManager, readModelPort);

    @Test
    void evictsRowScopeCache() {
        Cache cache = Mockito.mock(Cache.class);
        when(cacheManager.getCache(CacheNames.ORGANIZATION_ROW_SCOPE)).thenReturn(cache);

        handler.handle(new CacheInvalidationEvent(CacheInvalidationType.ROW_SCOPE, "t1", "s1", 1L, Instant.now()));

        verify(cache).clear();
    }

    @Test
    void evictsOrgCacheAndReadModel() {
        Cache cache = Mockito.mock(Cache.class);
        when(cacheManager.getCache(CacheNames.DW_ORG_TREE)).thenReturn(cache);
        when(readModelPort.isEnabled()).thenReturn(true);

        handler.handle(new CacheInvalidationEvent(CacheInvalidationType.ORGANIZATION, "t1", "s1", 1L, Instant.now()));

        verify(cache).clear();
        verify(readModelPort).evict();
        verify(readModelPort).rebuild();
    }

    @Test
    void permissionMenuClearsUserCache() {
        Cache cache = Mockito.mock(Cache.class);
        when(cacheManager.getCache(CacheNames.USER_DETAILS)).thenReturn(cache);

        handler.handle(new CacheInvalidationEvent(CacheInvalidationType.PERMISSION_MENU, "t1", "s1", 1L, Instant.now()));

        verify(cache, times(1)).clear();
    }
}
