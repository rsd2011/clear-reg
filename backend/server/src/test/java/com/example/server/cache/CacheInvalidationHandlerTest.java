package com.example.server.cache;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyString;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import com.example.common.cache.CacheInvalidationEvent;
import com.example.common.cache.CacheInvalidationType;
import com.example.common.cache.CacheNames;
import com.example.dw.application.readmodel.MenuReadModelPort;
import com.example.dw.application.readmodel.OrganizationReadModelPort;
import com.example.dw.application.readmodel.PermissionMenuReadModelPort;

class CacheInvalidationHandlerTest {

    private final CacheManager cacheManager = Mockito.mock(CacheManager.class);
    private final OrganizationReadModelPort readModelPort = Mockito.mock(OrganizationReadModelPort.class);
    private final MenuReadModelPort menuReadModelPort = Mockito.mock(MenuReadModelPort.class);
    private final PermissionMenuReadModelPort permissionMenuReadModelPort = Mockito.mock(PermissionMenuReadModelPort.class);
    private final CacheInvalidationHandler handler = new CacheInvalidationHandler(cacheManager, readModelPort, menuReadModelPort, permissionMenuReadModelPort);

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
    @DisplayName("Read model 비활성화 상태에서는 캐시만 비우고 rebuild를 호출하지 않는다")
    void orgEventSkipsReadModelWhenDisabled() {
        Cache cache = Mockito.mock(Cache.class);
        when(cacheManager.getCache(CacheNames.DW_ORG_TREE)).thenReturn(cache);
        when(readModelPort.isEnabled()).thenReturn(false);

        handler.handle(new CacheInvalidationEvent(CacheInvalidationType.ORGANIZATION, "t1", "s1", 1L, Instant.now()));

        verify(cache).clear();
        verify(readModelPort, times(0)).rebuild();
    }

    @Test
    void permissionMenuClearsUserCache() {
        Cache cache = Mockito.mock(Cache.class);
        when(cacheManager.getCache(CacheNames.USER_DETAILS)).thenReturn(cache);
        when(menuReadModelPort.isEnabled()).thenReturn(true);
        when(permissionMenuReadModelPort.isEnabled()).thenReturn(true);

        handler.handle(new CacheInvalidationEvent(CacheInvalidationType.PERMISSION_MENU, "t1", "user-1", 1L, Instant.now()));

        verify(cache, times(1)).clear();
        verify(menuReadModelPort).evict();
        verify(menuReadModelPort).rebuild();
        verify(permissionMenuReadModelPort).evict("user-1");
        verify(permissionMenuReadModelPort).rebuild("user-1");
    }

    @Test
    @DisplayName("PERMISSION_MENU 이벤트에서 scopeId가 없으면 사용자별 캐시는 건너뛴다")
    void permissionMenuSkipsPrincipalWhenScopeMissing() {
        Cache cache = Mockito.mock(Cache.class);
        when(cacheManager.getCache(CacheNames.USER_DETAILS)).thenReturn(cache);
        when(menuReadModelPort.isEnabled()).thenReturn(true);
        when(permissionMenuReadModelPort.isEnabled()).thenReturn(true);

        handler.handle(new CacheInvalidationEvent(CacheInvalidationType.PERMISSION_MENU, "t1", null, 1L, Instant.now()));

        verify(cache).clear();
        verify(menuReadModelPort).evict();
        verify(menuReadModelPort).rebuild();
        verify(permissionMenuReadModelPort, times(0)).evict(anyString());
        verify(permissionMenuReadModelPort, times(0)).rebuild(anyString());
    }

    @Test
    @DisplayName("메뉴 리드모델이 비활성화되면 캐시만 비운다")
    void permissionMenuSkipsRebuildWhenDisabled() {
        Cache cache = Mockito.mock(Cache.class);
        when(cacheManager.getCache(CacheNames.USER_DETAILS)).thenReturn(cache);
        when(menuReadModelPort.isEnabled()).thenReturn(false);
        when(permissionMenuReadModelPort.isEnabled()).thenReturn(false);

        handler.handle(new CacheInvalidationEvent(CacheInvalidationType.PERMISSION_MENU, "t1", "u1", 1L, Instant.now()));

        verify(cache).clear();
        verify(menuReadModelPort, times(0)).evict();
        verify(permissionMenuReadModelPort, times(0)).evict(anyString());
    }

    @Test
    @DisplayName("null 이벤트는 무시된다")
    void nullEventIsIgnored() {
        handler.handle(null);
    }
}
