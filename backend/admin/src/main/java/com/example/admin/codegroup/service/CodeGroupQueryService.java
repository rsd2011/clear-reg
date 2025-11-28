package com.example.admin.codegroup.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.example.common.cache.CacheNames;
import com.example.dw.application.DwCommonCodeDirectoryService;
import com.example.dw.application.DwCommonCodeSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.common.codegroup.annotation.ManagedCode;
import com.example.admin.codegroup.domain.CodeGroup;
import com.example.admin.codegroup.domain.CodeGroupSource;
import com.example.admin.codegroup.domain.CodeItem;
import com.example.admin.codegroup.domain.DynamicCodeType;
import com.example.admin.codegroup.dto.CodeGroupItem;
import com.example.admin.codegroup.dto.CodeGroupItemResponse;
import com.example.admin.codegroup.dto.CodeGroupInfo;
import com.example.admin.codegroup.dto.MigrationStatusResponse;
import com.example.admin.codegroup.dto.MigrationStatusResponse.CodeGroupStatus;
import com.example.admin.codegroup.dto.MigrationStatusResponse.SyncedGroupStatus;
import com.example.admin.codegroup.registry.StaticCodeRegistry;
import com.example.admin.codegroup.repository.CodeGroupRepository;
import com.example.admin.codegroup.repository.CodeItemRepository;
import com.example.admin.codegroup.util.CodeGroupUtils;
import com.example.admin.approval.service.ApprovalGroupService;
import com.example.admin.approval.dto.ApprovalGroupSummaryResponse;
import com.example.admin.codegroup.locale.LocaleCodeProvider;
import com.example.admin.codegroup.locale.LocaleCodeProvider.LocaleCountryEntry;
import com.example.admin.codegroup.locale.LocaleCodeProvider.LocaleLanguageEntry;

/**
 * 코드 그룹 통합 조회 서비스.
 *
 * <p>정적 Enum, 동적 DB 코드, DW 코드 등 모든 소스의 코드를
 * 통합하여 조회하는 서비스입니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeGroupQueryService {

    private final DwCommonCodeDirectoryService dwCommonCodeDirectoryService;
    private final CodeGroupService codeGroupService;
    private final StaticCodeRegistry staticCodeRegistry;
    private final CodeGroupRepository codeGroupRepository;
    private final CodeItemRepository codeItemRepository;
    private final ApprovalGroupService approvalGroupService;
    private final LocaleCodeProvider localeCodeProvider;

    // ========== 통합 조회 API ==========

    /**
     * 모든 소스의 코드를 그룹 코드별로 집계하여 반환.
     * 화면 초기 로드용.
     *
     * @return 그룹 코드별 코드 아이템 맵
     */
    @Cacheable(cacheNames = CacheNames.COMMON_CODE_AGGREGATES, key = "'ALL'")
    public Map<String, List<CodeGroupItem>> aggregateAll() {
        Map<String, List<CodeGroupItem>> result = new LinkedHashMap<>();

        // 1. 정적 Enum 코드 수집
        for (Class<? extends Enum<?>> enumClass : staticCodeRegistry.getRegisteredEnums()) {
            String groupCode = CodeGroupUtils.toGroupCode(enumClass);
            List<CodeGroupItem> items = staticCodeRegistry.getCodeGroupItems(enumClass);
            result.put(groupCode, items);
        }

        // 2. 동적 DB 코드 수집
        List<CodeGroup> dbGroups = codeGroupRepository.findAllBySourceWithItems(CodeGroupSource.DYNAMIC_DB);
        for (CodeGroup group : dbGroups) {
            if (!result.containsKey(group.getGroupCode())) {
                List<CodeGroupItem> items = group.getItems().stream()
                        .filter(CodeItem::isActive)
                        .map(this::toCodeGroupItem)
                        .toList();
                result.put(group.getGroupCode(), items);
            }
        }

        // 3. 승인 그룹 코드 수집
        List<ApprovalGroupSummaryResponse> groups = approvalGroupService.listGroupSummary(true);
        if (!groups.isEmpty()) {
            int order = 0;
            List<CodeGroupItem> approvalGroupItems = new ArrayList<>();
            for (ApprovalGroupSummaryResponse group : groups) {
                approvalGroupItems.add(CodeGroupItem.ofApprovalGroup(
                        group.groupCode(),
                        group.name(),
                        order++,
                        true,
                        null
                ));
            }
            result.put("APPROVAL_GROUP", approvalGroupItems);
        }

        log.debug("Aggregated {} code groups", result.size());
        return result;
    }

    /**
     * 특정 그룹 코드의 코드 목록 조회.
     *
     * @param groupCode 그룹 코드명
     * @return 코드 아이템 목록 (없으면 빈 리스트)
     */
    @Cacheable(cacheNames = CacheNames.COMMON_CODE_AGGREGATES, key = "#groupCode.toUpperCase()")
    public List<CodeGroupItem> findByGroupCode(String groupCode) {
        String normalized = normalize(groupCode);

        // 1. 정적 Enum에서 찾기
        return staticCodeRegistry.findByCodeType(normalized)
                .map(staticCodeRegistry::getCodeGroupItems)
                .orElseGet(() -> {
                    // 2. 동적 DB에서 찾기
                    List<CodeItem> items = codeGroupService.findActiveItems(CodeGroupSource.DYNAMIC_DB, normalized);
                    return items.stream()
                            .map(this::toCodeGroupItem)
                            .toList();
                });
    }

    /**
     * 여러 그룹 코드 일괄 조회.
     *
     * @param groupCodes 그룹 코드명 목록
     * @return 그룹 코드별 코드 아이템 맵
     */
    public Map<String, List<CodeGroupItem>> findByGroupCodes(List<String> groupCodes) {
        Map<String, List<CodeGroupItem>> result = new LinkedHashMap<>();
        for (String groupCode : groupCodes) {
            List<CodeGroupItem> items = findByGroupCode(groupCode);
            if (!items.isEmpty()) {
                result.put(groupCode, items);
            }
        }
        return result;
    }

    /**
     * 그룹 코드 메타정보 목록 조회 (관리 화면용).
     *
     * @return 그룹 코드 메타정보 목록
     */
    public List<CodeGroupInfo> getCodeGroupInfos() {
        List<CodeGroupInfo> infos = new ArrayList<>();

        // 1. 정적 Enum 타입 정보
        for (Class<? extends Enum<?>> enumClass : staticCodeRegistry.getRegisteredEnums()) {
            String groupCode = CodeGroupUtils.toGroupCode(enumClass);
            ManagedCode annotation = enumClass.getAnnotation(ManagedCode.class);

            String displayName = annotation != null && !annotation.displayName().isEmpty()
                    ? annotation.displayName() : groupCode;
            String description = annotation != null ? annotation.description() : "";
            String group = annotation != null && !annotation.group().isEmpty()
                    ? annotation.group() : "GENERAL";

            int itemCount = staticCodeRegistry.getCodeGroupItems(enumClass).size();

            infos.add(CodeGroupInfo.ofStaticEnum(groupCode, displayName, description, group, itemCount));
        }

        // 2. 동적 DB 타입 정보 (DB에 데이터가 없어도 DynamicCodeType은 항상 표시)
        for (DynamicCodeType dynamicType : DynamicCodeType.values()) {
            String groupCode = dynamicType.name();
            long count = codeItemRepository.countBySourceAndGroupCode(CodeGroupSource.DYNAMIC_DB, groupCode);
            infos.add(CodeGroupInfo.ofDynamicDb(
                    groupCode,
                    groupCode,  // displayName으로 코드명 사용
                    "동적 관리 코드",
                    (int) count
            ));
        }

        // 정렬: 그룹 → 그룹코드명
        infos.sort(Comparator.comparing(CodeGroupInfo::group)
                .thenComparing(CodeGroupInfo::groupCode));

        return infos;
    }

    /**
     * 캐시 무효화.
     *
     * @param groupCode 무효화할 그룹 코드 (null이면 전체)
     */
    public void evictCache(String groupCode) {
        if (groupCode != null) {
            evictCacheForGroup(groupCode);
            staticCodeRegistry.invalidateCache(groupCode);
            log.info("Cache evicted for groupCode: {}", groupCode);
        } else {
            evictAllCaches();
            staticCodeRegistry.invalidateCache(null);
            log.info("All code caches evicted");
        }
    }

    @CacheEvict(cacheNames = CacheNames.COMMON_CODE_AGGREGATES, key = "#groupCode.toUpperCase()")
    public void evictCacheForGroup(String groupCode) {
        // Cache eviction by annotation
    }

    @CacheEvict(cacheNames = CacheNames.COMMON_CODE_AGGREGATES, allEntries = true)
    public void evictAllCaches() {
        // Cache eviction by annotation
    }

    // ========== 통합 코드 항목 조회 API (v2) ==========

    /**
     * 통합 코드 항목 목록 조회 (메인 API).
     *
     * <p>모든 소스의 코드를 플랫 리스트로 반환합니다. 페이징 없음.</p>
     *
     * @param sources   소스 필터 (null이면 전체)
     * @param groupCode 그룹 코드 필터 (null이면 전체)
     * @param active    활성 상태 필터 (null이면 전체)
     * @param search    검색어 (아이템코드/아이템명)
     * @return 코드 항목 응답 목록
     */
    public List<CodeGroupItemResponse> findAllItems(
            List<CodeGroupSource> sources,
            String groupCode,
            Boolean active,
            String search
    ) {
        List<CodeGroupItemResponse> result = new ArrayList<>();

        // DB 오버라이드 맵 구축 (groupCode -> itemCode -> entity)
        Map<String, Map<String, CodeItem>> dbOverrideMap = buildDbOverrideMap();

        Set<CodeGroupSource> sourceFilter = sources != null && !sources.isEmpty()
                ? new HashSet<>(sources) : null;

        // 1. Static Enum 소스 처리
        if (sourceFilter == null || sourceFilter.contains(CodeGroupSource.STATIC_ENUM)) {
            result.addAll(collectStaticEnumItems(dbOverrideMap, groupCode, active, search));
        }

        // 2. Dynamic DB 소스 처리
        if (sourceFilter == null || sourceFilter.contains(CodeGroupSource.DYNAMIC_DB)) {
            result.addAll(collectDynamicDbItems(dbOverrideMap, groupCode, active, search));
        }

        // 3. DW 소스 처리
        if (sourceFilter == null || sourceFilter.contains(CodeGroupSource.DW)) {
            result.addAll(collectDwItems(groupCode, active, search));
        }

        // 4. Approval Group 소스 처리
        if (sourceFilter == null || sourceFilter.contains(CodeGroupSource.APPROVAL_GROUP)) {
            result.addAll(collectApprovalGroupItems(groupCode, active, search));
        }

        // 5. Locale Country 소스 처리
        if (sourceFilter == null || sourceFilter.contains(CodeGroupSource.LOCALE_COUNTRY)) {
            result.addAll(collectLocaleCountryItems(groupCode, active, search));
        }

        // 6. Locale Language 소스 처리
        if (sourceFilter == null || sourceFilter.contains(CodeGroupSource.LOCALE_LANGUAGE)) {
            result.addAll(collectLocaleLanguageItems(groupCode, active, search));
        }

        // 정렬: groupCode -> displayOrder -> itemCode
        result.sort(Comparator
                .comparing(CodeGroupItemResponse::groupCode)
                .thenComparingInt(item -> item.displayOrder() != null ? item.displayOrder() : Integer.MAX_VALUE)
                .thenComparing(CodeGroupItemResponse::itemCode));

        return result;
    }

    /**
     * 마이그레이션 상태 조회.
     *
     * <p>Enum과 DB 간의 groupCode 불일치 상태를 반환합니다.</p>
     *
     * @return 마이그레이션 상태 응답
     */
    public MigrationStatusResponse getMigrationStatus() {
        // 1. Enum에 등록된 모든 groupCode 수집
        Set<String> enumGroupCodes = staticCodeRegistry.getRegisteredEnums().stream()
                .map(CodeGroupUtils::toGroupCode)
                .collect(Collectors.toSet());

        // 2. DB에 저장된 모든 groupCode 수집 (STATIC_ENUM 소스의 오버라이드 포함)
        List<String> dbGroupCodes = codeGroupRepository.findDistinctGroupCodes();
        Set<String> dbGroupCodeSet = new HashSet<>(dbGroupCodes);

        // 3. 분류
        List<CodeGroupStatus> enumOnlyGroups = new ArrayList<>();
        List<CodeGroupStatus> dbOnlyGroups = new ArrayList<>();
        List<SyncedGroupStatus> syncedGroups = new ArrayList<>();

        // Enum에만 존재하는 그룹
        for (String groupCode : enumGroupCodes) {
            boolean hasDbRecords = dbGroupCodeSet.contains(groupCode);
            if (!hasDbRecords) {
                int itemCount = staticCodeRegistry.findByCodeType(groupCode)
                        .map(enumClass -> staticCodeRegistry.getCodeGroupItems(enumClass).size())
                        .orElse(0);
                enumOnlyGroups.add(CodeGroupStatus.of(groupCode, itemCount, "신규 Enum (DB에 오버라이드 레코드 없음)"));
            } else {
                // 동기화된 그룹
                int enumCount = staticCodeRegistry.findByCodeType(groupCode)
                        .map(enumClass -> staticCodeRegistry.getCodeGroupItems(enumClass).size())
                        .orElse(0);
                long dbOverrideCount = codeItemRepository.countBySourceAndGroupCode(CodeGroupSource.STATIC_ENUM, groupCode);
                syncedGroups.add(SyncedGroupStatus.of(groupCode, enumCount, (int) dbOverrideCount));
            }
        }

        // DB에만 존재하는 그룹 (Enum이 삭제되었거나 이름 변경됨)
        for (String groupCode : dbGroupCodeSet) {
            if (!enumGroupCodes.contains(groupCode) && !isDynamicCodeType(groupCode)) {
                long count = codeItemRepository.countBySourceAndGroupCode(CodeGroupSource.STATIC_ENUM, groupCode);
                dbOnlyGroups.add(CodeGroupStatus.of(groupCode, (int) count, "Enum에서 삭제됨 또는 이름 변경됨"));
            }
        }

        // 정렬
        enumOnlyGroups.sort(Comparator.comparing(CodeGroupStatus::groupCode));
        dbOnlyGroups.sort(Comparator.comparing(CodeGroupStatus::groupCode));
        syncedGroups.sort(Comparator.comparing(SyncedGroupStatus::groupCode));

        return new MigrationStatusResponse(enumOnlyGroups, dbOnlyGroups, syncedGroups);
    }

    // ========== Private Helper Methods ==========

    /**
     * DB 오버라이드 맵 구축 (Static Enum용).
     */
    private Map<String, Map<String, CodeItem>> buildDbOverrideMap() {
        List<CodeGroup> allGroups = codeGroupRepository.findAllBySourceWithItems(CodeGroupSource.STATIC_ENUM);
        Map<String, Map<String, CodeItem>> result = new LinkedHashMap<>();
        for (CodeGroup group : allGroups) {
            Map<String, CodeItem> itemMap = group.getItems().stream()
                    .collect(Collectors.toMap(
                            CodeItem::getItemCode,
                            Function.identity(),
                            (existing, replacement) -> existing
                    ));
            result.put(group.getGroupCode(), itemMap);
        }
        return result;
    }

    /**
     * Static Enum 코드 수집.
     */
    private List<CodeGroupItemResponse> collectStaticEnumItems(
            Map<String, Map<String, CodeItem>> dbOverrideMap,
            String groupCodeFilter,
            Boolean activeFilter,
            String search
    ) {
        List<CodeGroupItemResponse> result = new ArrayList<>();

        for (Class<? extends Enum<?>> enumClass : staticCodeRegistry.getRegisteredEnums()) {
            String enumGroupCode = CodeGroupUtils.toGroupCode(enumClass);

            // groupCode 필터
            if (groupCodeFilter != null && !enumGroupCode.equalsIgnoreCase(groupCodeFilter)) {
                continue;
            }

            List<CodeGroupItem> enumItems = staticCodeRegistry.getCodeGroupItems(enumClass);
            Map<String, CodeItem> overrides = dbOverrideMap.getOrDefault(enumGroupCode, Map.of());

            for (CodeGroupItem item : enumItems) {
                CodeItem override = overrides.get(item.itemCode());
                CodeGroupItemResponse response = createStaticEnumResponse(item, override);

                // active 필터
                if (activeFilter != null && response.active() != activeFilter) {
                    continue;
                }

                // 검색 필터
                if (search != null && !search.isBlank()) {
                    String lowerSearch = search.toLowerCase(Locale.ROOT);
                    boolean matches = response.itemCode().toLowerCase(Locale.ROOT).contains(lowerSearch)
                            || (response.itemName() != null && response.itemName().toLowerCase(Locale.ROOT).contains(lowerSearch));
                    if (!matches) {
                        continue;
                    }
                }

                result.add(response);
            }
        }

        return result;
    }

    /**
     * Static Enum 응답 생성 (DB 오버라이드 적용).
     */
    private CodeGroupItemResponse createStaticEnumResponse(CodeGroupItem enumItem, CodeItem override) {
        if (override != null) {
            // DB 오버라이드 값 적용
            return new CodeGroupItemResponse(
                    override.getId(),
                    enumItem.groupCode(),
                    enumItem.itemCode(),
                    override.getItemName() != null ? override.getItemName() : enumItem.itemName(),
                    override.getDisplayOrder() != 0 ? override.getDisplayOrder() : enumItem.displayOrder(),
                    override.isActive(),
                    CodeGroupSource.STATIC_ENUM,
                    override.getDescription() != null ? override.getDescription() : enumItem.description(),
                    override.getMetadataJson(),
                    true,  // editable
                    true,  // deletable (STATIC_ENUM은 삭제 가능)
                    true,  // hasDbOverride
                    false, // builtIn - STATIC_ENUM은 builtIn 아님
                    override.getUpdatedAt(),
                    override.getUpdatedBy()
            );
        } else {
            // Enum 기본값 사용
            return CodeGroupItemResponse.from(enumItem);
        }
    }

    /**
     * Dynamic DB 코드 수집.
     */
    private List<CodeGroupItemResponse> collectDynamicDbItems(
            Map<String, Map<String, CodeItem>> dbOverrideMap,
            String groupCodeFilter,
            Boolean activeFilter,
            String search
    ) {
        List<CodeGroupItemResponse> result = new ArrayList<>();

        // Static Enum에 등록된 groupCode는 제외 (이미 Static으로 처리됨)
        Set<String> staticGroupCodes = staticCodeRegistry.getRegisteredEnums().stream()
                .map(CodeGroupUtils::toGroupCode)
                .collect(Collectors.toSet());

        for (DynamicCodeType dynamicType : DynamicCodeType.values()) {
            String dynamicGroupCode = dynamicType.name();

            // groupCode 필터
            if (groupCodeFilter != null && !dynamicGroupCode.equalsIgnoreCase(groupCodeFilter)) {
                continue;
            }

            // Static Enum과 중복 체크
            if (staticGroupCodes.contains(dynamicGroupCode)) {
                continue;
            }

            List<CodeItem> items = codeGroupService.findAllItems(CodeGroupSource.DYNAMIC_DB, dynamicGroupCode);

            // DB에 데이터가 없는 경우 빈 placeholder row 생성
            if (items.isEmpty()) {
                // active=false 필터인 경우 빈 row도 제외
                if (activeFilter != null && activeFilter) {
                    continue;
                }

                // 검색 필터가 있는 경우 빈 row는 제외
                if (search != null && !search.isBlank()) {
                    continue;
                }

                // 빈 placeholder row 생성
                CodeGroupItemResponse emptyRow = new CodeGroupItemResponse(
                        null,
                        dynamicGroupCode,
                        null,  // itemCode - null로 빈 row 표시
                        null,  // itemName
                        0,     // displayOrder
                        true,  // active
                        CodeGroupSource.DYNAMIC_DB,
                        null,  // description
                        null,  // metadataJson
                        true,  // editable
                        false, // deletable - 빈 row는 삭제 불가
                        false, // hasDbOverride
                        false, // builtIn - DYNAMIC_DB는 builtIn 아님
                        null,  // updatedAt
                        null   // updatedBy
                );
                result.add(emptyRow);
                continue;
            }

            for (CodeItem item : items) {
                CodeGroupItemResponse response = CodeGroupItemResponse.fromEntity(item, CodeGroupSource.DYNAMIC_DB);

                // active 필터
                if (activeFilter != null && response.active() != activeFilter) {
                    continue;
                }

                // 검색 필터
                if (search != null && !search.isBlank()) {
                    String lowerSearch = search.toLowerCase(Locale.ROOT);
                    boolean matches = response.itemCode().toLowerCase(Locale.ROOT).contains(lowerSearch)
                            || (response.itemName() != null && response.itemName().toLowerCase(Locale.ROOT).contains(lowerSearch));
                    if (!matches) {
                        continue;
                    }
                }

                result.add(response);
            }
        }

        return result;
    }

    /**
     * DW 코드 수집.
     */
    private List<CodeGroupItemResponse> collectDwItems(
            String groupCodeFilter,
            Boolean activeFilter,
            String search
    ) {
        List<CodeGroupItemResponse> result = new ArrayList<>();

        // DW는 활성 코드만 제공
        if (activeFilter != null && !activeFilter) {
            return result;
        }

        // TODO: DW 코드 타입 목록 조회 API가 필요하면 추가
        // 현재는 groupCodeFilter가 지정된 경우만 처리
        if (groupCodeFilter != null) {
            List<DwCommonCodeSnapshot> dwCodes = dwCommonCodeDirectoryService.findActive(groupCodeFilter);
            for (DwCommonCodeSnapshot snapshot : dwCodes) {
                CodeGroupItem item = CodeGroupItem.ofDw(
                        groupCodeFilter,
                        snapshot.codeValue(),
                        snapshot.codeName(),
                        snapshot.displayOrder(),
                        snapshot.category(),
                        snapshot.metadataJson()
                );
                CodeGroupItemResponse response = CodeGroupItemResponse.from(item);

                // 검색 필터
                if (search != null && !search.isBlank()) {
                    String lowerSearch = search.toLowerCase(Locale.ROOT);
                    boolean matches = response.itemCode().toLowerCase(Locale.ROOT).contains(lowerSearch)
                            || (response.itemName() != null && response.itemName().toLowerCase(Locale.ROOT).contains(lowerSearch));
                    if (!matches) {
                        continue;
                    }
                }

                result.add(response);
            }
        }

        return result;
    }

    /**
     * Approval Group 코드 수집.
     */
    private List<CodeGroupItemResponse> collectApprovalGroupItems(
            String groupCodeFilter,
            Boolean activeFilter,
            String search
    ) {
        List<CodeGroupItemResponse> result = new ArrayList<>();

        // APPROVAL_GROUP groupCode 필터 (groupCode가 지정되고 APPROVAL_GROUP이 아니면 스킵)
        if (groupCodeFilter != null && !"APPROVAL_GROUP".equalsIgnoreCase(groupCodeFilter)) {
            return result;
        }

        // activeFilter가 false면 스킵 (승인 그룹은 활성 그룹만 조회)
        if (activeFilter != null && !activeFilter) {
            return result;
        }

        List<ApprovalGroupSummaryResponse> groups = approvalGroupService.listGroupSummary(true);
        int order = 0;
        for (ApprovalGroupSummaryResponse group : groups) {
            CodeGroupItem item = CodeGroupItem.ofApprovalGroup(
                    group.groupCode(),
                    group.name(),
                    order++,
                    true,
                    null
            );
            CodeGroupItemResponse response = CodeGroupItemResponse.from(item);

            // 검색 필터
            if (search != null && !search.isBlank()) {
                String lowerSearch = search.toLowerCase(Locale.ROOT);
                boolean matches = response.itemCode().toLowerCase(Locale.ROOT).contains(lowerSearch)
                        || (response.itemName() != null && response.itemName().toLowerCase(Locale.ROOT).contains(lowerSearch));
                if (!matches) {
                    continue;
                }
            }

            result.add(response);
        }

        return result;
    }


    /**
     * Locale Country 코드 수집.
     */
    private List<CodeGroupItemResponse> collectLocaleCountryItems(
            String groupCodeFilter,
            Boolean activeFilter,
            String search
    ) {
        List<CodeGroupItemResponse> result = new ArrayList<>();

        // groupCode 필터 (LOCALE_COUNTRY가 아니면 스킵)
        if (groupCodeFilter != null && !LocaleCodeProvider.GROUP_CODE_COUNTRY.equalsIgnoreCase(groupCodeFilter)) {
            return result;
        }

        // Locale은 기본적으로 활성 항목만 반환 (activeFilter=false면 빈 결과)
        if (activeFilter != null && !activeFilter) {
            return result;
        }

        // LocaleCodeProvider에서 국가 목록 조회 (검색어가 있으면 검색)
        List<LocaleCountryEntry> countries = (search != null && !search.isBlank())
                ? localeCodeProvider.searchCountries(search)
                : localeCodeProvider.getCountries();

        for (LocaleCountryEntry entry : countries) {
            CodeGroupItemResponse response = createLocaleCountryResponse(entry);
            result.add(response);
        }

        return result;
    }

    /**
     * Locale Language 코드 수집.
     */
    private List<CodeGroupItemResponse> collectLocaleLanguageItems(
            String groupCodeFilter,
            Boolean activeFilter,
            String search
    ) {
        List<CodeGroupItemResponse> result = new ArrayList<>();

        // groupCode 필터 (LOCALE_LANGUAGE가 아니면 스킵)
        if (groupCodeFilter != null && !LocaleCodeProvider.GROUP_CODE_LANGUAGE.equalsIgnoreCase(groupCodeFilter)) {
            return result;
        }

        // Locale은 기본적으로 활성 항목만 반환 (activeFilter=false면 빈 결과)
        if (activeFilter != null && !activeFilter) {
            return result;
        }

        // LocaleCodeProvider에서 언어 목록 조회 (검색어가 있으면 검색)
        List<LocaleLanguageEntry> languages = (search != null && !search.isBlank())
                ? localeCodeProvider.searchLanguages(search)
                : localeCodeProvider.getLanguages();

        for (LocaleLanguageEntry entry : languages) {
            CodeGroupItemResponse response = createLocaleLanguageResponse(entry);
            result.add(response);
        }

        return result;
    }

    /**
     * LocaleCountryEntry를 CodeGroupItemResponse로 변환.
     */
    private CodeGroupItemResponse createLocaleCountryResponse(LocaleCountryEntry entry) {
        String metadataJson = null;
        if (entry.metadata() != null) {
            try {
                metadataJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(entry.metadata());
            } catch (Exception e) {
                log.warn("국가 메타데이터 직렬화 실패: {}", entry.code(), e);
            }
        }

        return new CodeGroupItemResponse(
                null,  // id - LocaleCodeProvider는 DB ID를 반환하지 않음
                LocaleCodeProvider.GROUP_CODE_COUNTRY,
                entry.code(),
                entry.name(),
                0,  // displayOrder - 이름 정렬 사용
                true,  // active
                CodeGroupSource.LOCALE_COUNTRY,
                null,  // description
                metadataJson,
                true,  // editable
                true,  // deletable
                entry.hasOverride(),  // hasDbOverride
                entry.builtIn(),  // builtIn
                null,  // updatedAt
                null   // updatedBy
        );
    }

    /**
     * LocaleLanguageEntry를 CodeGroupItemResponse로 변환.
     */
    private CodeGroupItemResponse createLocaleLanguageResponse(LocaleLanguageEntry entry) {
        String metadataJson = null;
        if (entry.metadata() != null) {
            try {
                metadataJson = new ObjectMapper().writeValueAsString(entry.metadata());
            } catch (Exception e) {
                log.warn("언어 메타데이터 직렬화 실패: {}", entry.code(), e);
            }
        }

        return new CodeGroupItemResponse(
                null,  // id - LocaleCodeProvider는 DB ID를 반환하지 않음
                LocaleCodeProvider.GROUP_CODE_LANGUAGE,
                entry.code(),
                entry.name(),
                0,  // displayOrder - 이름 정렬 사용
                true,  // active
                CodeGroupSource.LOCALE_LANGUAGE,
                null,  // description
                metadataJson,
                true,  // editable
                true,  // deletable
                entry.hasOverride(),  // hasDbOverride
                entry.builtIn(),  // builtIn
                null,  // updatedAt
                null   // updatedBy
        );
    }

    /**
     * 동적 코드 타입인지 확인.
     */
    private boolean isDynamicCodeType(String groupCode) {
        return DynamicCodeType.fromCode(groupCode) != null;
    }

    private CodeGroupItem toCodeGroupItem(CodeItem item) {
        return new CodeGroupItem(
                item.getGroupCode(),
                item.getItemCode(),
                item.getItemName(),
                item.getDisplayOrder(),
                item.isActive(),
                CodeGroupSource.DYNAMIC_DB,
                item.getDescription(),
                item.getMetadataJson()
        );
    }

    private String normalize(String groupCode) {
        if (groupCode == null) {
            throw new IllegalArgumentException("groupCode must not be null");
        }
        return groupCode.toUpperCase(Locale.ROOT);
    }
}
