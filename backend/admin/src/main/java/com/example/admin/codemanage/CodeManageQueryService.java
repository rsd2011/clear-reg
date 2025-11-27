package com.example.admin.codemanage;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.example.common.cache.CacheNames;
import com.example.dw.application.DwCommonCodeDirectoryService;
import com.example.dw.application.DwCommonCodeSnapshot;
import com.example.admin.codemanage.dto.CodeManageItem;
import com.example.admin.codemanage.model.CodeManageKind;
import com.example.admin.codemanage.model.CodeManageSource;
import com.example.admin.codemanage.model.SystemCommonCode;

@Service
@RequiredArgsConstructor
public class CodeManageQueryService {

    private final DwCommonCodeDirectoryService dwCommonCodeDirectoryService;
    private final SystemCommonCodeService systemCommonCodeService;

    @Cacheable(cacheNames = CacheNames.COMMON_CODE_AGGREGATES,
            key = "#codeType.toUpperCase() + '|' + #includeSystem + '|' + #includeDw")
    public List<CodeManageItem> aggregate(String codeType, boolean includeSystem, boolean includeDw) {
        String normalized = normalize(codeType);
        Map<String, CodeManageItem> resolved = new LinkedHashMap<>();
        if (includeSystem) {
            List<SystemCommonCode> systemCodes = systemCommonCodeService.findActive(normalized);
            for (SystemCommonCode code : systemCodes) {
                resolved.put(code.getCodeValue(), new CodeManageItem(
                        normalized,
                        code.getCodeValue(),
                        code.getCodeName(),
                        code.getDisplayOrder(),
                        code.isActive(),
                        code.getCodeKind(),
                        CodeManageSource.SYSTEM,
                        code.getDescription(),
                        code.getMetadataJson()));
            }
        }
        if (includeDw) {
            List<DwCommonCodeSnapshot> dwCodes = dwCommonCodeDirectoryService.findActive(normalized);
            for (DwCommonCodeSnapshot snapshot : dwCodes) {
                resolved.putIfAbsent(snapshot.codeValue(), new CodeManageItem(
                        normalized,
                        snapshot.codeValue(),
                        snapshot.codeName(),
                        snapshot.displayOrder(),
                        true,
                        CodeManageKind.FEDERATED,
                        CodeManageSource.DW,
                        snapshot.category(),
                        snapshot.metadataJson()));
            }
        }
        return resolved.values().stream()
                .sorted(Comparator
                        .comparingInt((CodeManageItem item) -> item.displayOrder() == null ? Integer.MAX_VALUE : item.displayOrder())
                        .thenComparing(CodeManageItem::codeValue))
                .toList();
    }

    @CacheEvict(cacheNames = CacheNames.COMMON_CODE_AGGREGATES, allEntries = true)
    public void evictAll() {
        // eviction only
    }

    private String normalize(String codeType) {
        if (codeType == null) {
            throw new IllegalArgumentException("codeType must not be null");
        }
        return codeType.toUpperCase(Locale.ROOT);
    }
}
