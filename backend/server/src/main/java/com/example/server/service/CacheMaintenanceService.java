package com.example.server.service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.example.common.cache.CacheNames;
import com.example.dw.application.DwCommonCodeDirectoryService;
import com.example.dw.application.DwEmployeeDirectoryService;
import com.example.dw.application.DwOrganizationTreeService;
import com.example.dw.application.readmodel.OrganizationReadModelPort;
import com.example.admin.codemanage.CodeManageQueryService;
import com.example.admin.codemanage.SystemCommonCodeService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CacheMaintenanceService {

    private final CacheManager cacheManager;
    private final DwEmployeeDirectoryService employeeDirectoryService;
    private final DwOrganizationTreeService organizationTreeService;
    private final DwCommonCodeDirectoryService dwCommonCodeDirectoryService;
    private final SystemCommonCodeService systemCommonCodeService;
    private final CodeManageQueryService codeManageQueryService;
    private final OrganizationReadModelPort organizationReadModelPort;

    public CacheMaintenanceService(CacheManager cacheManager,
                                   DwEmployeeDirectoryService employeeDirectoryService,
                                   DwOrganizationTreeService organizationTreeService,
                                   DwCommonCodeDirectoryService dwCommonCodeDirectoryService,
                                   SystemCommonCodeService systemCommonCodeService,
                                   CodeManageQueryService codeManageQueryService,
                                   @Nullable OrganizationReadModelPort organizationReadModelPort) {
        this.cacheManager = cacheManager;
        this.employeeDirectoryService = employeeDirectoryService;
        this.organizationTreeService = organizationTreeService;
        this.dwCommonCodeDirectoryService = dwCommonCodeDirectoryService;
        this.systemCommonCodeService = systemCommonCodeService;
        this.codeManageQueryService = codeManageQueryService;
        this.organizationReadModelPort = organizationReadModelPort;
    }

    public List<String> clearCaches(List<String> requestedTargets) {
        List<String> cleared = new ArrayList<>();
        if (requestedTargets == null || requestedTargets.isEmpty()) {
            clearTarget(CacheTarget.ALL, cleared);
            return cleared;
        }
        for (String requested : requestedTargets) {
            CacheTarget.from(requested).ifPresentOrElse(
                    target -> clearTarget(target, cleared),
                    () -> log.warn("Unsupported cache target '{}'", requested));
        }
        return cleared;
    }

    private void clearTarget(CacheTarget target, List<String> cleared) {
        if (target == CacheTarget.ALL) {
            EnumSet.allOf(CacheTarget.class).stream()
                    .filter(each -> each != CacheTarget.ALL)
                    .forEach(each -> clearTarget(each, cleared));
            return;
        }
        switch (target) {
            case DW_EMPLOYEES -> employeeDirectoryService.evictAll();
            case DW_ORG_TREE -> organizationTreeService.evict();
            case ORGANIZATION_ROW_SCOPE -> clearCache(CacheNames.ORGANIZATION_ROW_SCOPE);
            case LATEST_DW_BATCH -> clearCache(CacheNames.LATEST_DW_BATCH);
            case DW_COMMON_CODES -> dwCommonCodeDirectoryService.evictAll();
            case SYSTEM_COMMON_CODES -> systemCommonCodeService.evictAll();
            case COMMON_CODE_AGGREGATES -> codeManageQueryService.evictAll();
            case ORGANIZATION_READ_MODEL -> refreshOrganizationReadModel();
        }
        cleared.add(target.name());
        log.info("Cleared cache target {}", target.name());
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        } else {
            log.debug("Cache '{}' not found while attempting to clear", cacheName);
        }
    }

    private void refreshOrganizationReadModel() {
        if (organizationReadModelPort == null || !organizationReadModelPort.isEnabled()) {
            log.info("Organization read model port unavailable; skipping refresh");
            return;
        }
        organizationReadModelPort.evict();
        organizationReadModelPort.rebuild();
    }

    public enum CacheTarget {
        DW_EMPLOYEES,
        DW_ORG_TREE,
        ORGANIZATION_ROW_SCOPE,
        LATEST_DW_BATCH,
        DW_COMMON_CODES,
        SYSTEM_COMMON_CODES,
        COMMON_CODE_AGGREGATES,
        ORGANIZATION_READ_MODEL,
        ALL;

        static Optional<CacheTarget> from(String value) {
            if (value == null) {
                return Optional.empty();
            }
            try {
                return Optional.of(CacheTarget.valueOf(value.toUpperCase(Locale.US)));
            } catch (IllegalArgumentException ex) {
                return Optional.empty();
            }
        }
    }
}
