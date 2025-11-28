package com.example.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import com.example.common.cache.CacheNames;
import com.example.dw.application.DwCommonCodeDirectoryService;
import com.example.dw.application.DwEmployeeDirectoryService;
import com.example.dw.application.DwOrganizationTreeService;
import com.example.dw.application.readmodel.OrganizationReadModelPort;
import com.example.admin.codegroup.service.CodeGroupQueryService;
import com.example.admin.codegroup.service.CodeGroupService;
import com.example.server.service.CacheMaintenanceService.CacheTarget;

@ExtendWith(MockitoExtension.class)
@DisplayName("CacheMaintenanceService 테스트")
class CacheMaintenanceServiceTest {

    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache organizationRowScopeCache;
    @Mock
    private Cache latestDwBatchCache;
    @Mock
    private DwEmployeeDirectoryService employeeDirectoryService;
    @Mock
    private DwOrganizationTreeService organizationTreeService;
    @Mock
    private DwCommonCodeDirectoryService dwCommonCodeDirectoryService;
    @Mock
    private CodeGroupService codeGroupService;
    @Mock
    private CodeGroupQueryService codeGroupQueryService;
    @Mock
    private OrganizationReadModelPort organizationReadModelPort;

    private CacheMaintenanceService cacheMaintenanceService;

    @BeforeEach
    void setUp() {
        cacheMaintenanceService = new CacheMaintenanceService(
                cacheManager,
                employeeDirectoryService,
                organizationTreeService,
                dwCommonCodeDirectoryService,
                codeGroupService,
                codeGroupQueryService,
                organizationReadModelPort
        );
    }

    @Test
    @DisplayName("Given 대상 미지정 When clearCaches 호출 Then 모든 캐시를 순차적으로 비운다")
    void clearCachesWithoutTargetsClearsEveryKnownTarget() {
        when(cacheManager.getCache(CacheNames.ORGANIZATION_ROW_SCOPE)).thenReturn(organizationRowScopeCache);
        when(cacheManager.getCache(CacheNames.LATEST_DW_BATCH)).thenReturn(latestDwBatchCache);
        when(organizationReadModelPort.isEnabled()).thenReturn(true);

        List<String> clearedTargets = cacheMaintenanceService.clearCaches(null);

        assertThat(clearedTargets).containsExactlyInAnyOrder(
                "DW_EMPLOYEES",
                "DW_ORG_TREE",
                "ORGANIZATION_ROW_SCOPE",
                "LATEST_DW_BATCH",
                "DW_COMMON_CODES",
                "SYSTEM_COMMON_CODES",
                "COMMON_CODE_AGGREGATES",
                "ORGANIZATION_READ_MODEL"
        );
        verify(employeeDirectoryService).evictAll();
        verify(organizationTreeService).evict();
        verify(dwCommonCodeDirectoryService).evictAll();
        verify(codeGroupService).evictAll();
        verify(codeGroupQueryService).evictAllCaches();
        verify(organizationRowScopeCache).clear();
        verify(latestDwBatchCache).clear();
        verify(organizationReadModelPort).isEnabled();
        verify(organizationReadModelPort).evict();
        verify(organizationReadModelPort).rebuild();
        verifyNoMoreInteractions(organizationReadModelPort);
    }

    @Test
    @DisplayName("Given 알 수 없는 대상 When clearCaches 호출 Then 무시하고 정상 종료한다")
    void clearCachesSkipsUnknownTargetsGracefully() {
        List<String> clearedTargets = cacheMaintenanceService.clearCaches(List.of("unknown"));

        assertThat(clearedTargets).isEmpty();
        verifyNoInteractions(employeeDirectoryService, organizationTreeService, dwCommonCodeDirectoryService,
                codeGroupService, codeGroupQueryService, organizationReadModelPort);
    }

    @Test
    @DisplayName("Given 문자열 입력 When CacheTarget.from 호출 Then 대소문자와 잘못된 값을 처리한다")
    void cacheTargetFromHandlesCaseSensitivityAndInvalidValues() {
        assertThat(CacheTarget.from("dw_employees")).contains(CacheTarget.DW_EMPLOYEES);
        assertThat(CacheTarget.from("INVALID_TARGET")).isEmpty();
        assertThat(CacheTarget.from(null)).isEmpty();
    }
}
