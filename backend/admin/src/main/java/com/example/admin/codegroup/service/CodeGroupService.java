package com.example.admin.codegroup.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.example.common.cache.CacheNames;
import com.example.admin.codegroup.domain.CodeGroup;
import com.example.admin.codegroup.domain.CodeGroupSource;
import com.example.admin.codegroup.domain.CodeItem;
import com.example.admin.codegroup.event.CodeGroupChangedEvent;
import com.example.admin.codegroup.dto.MigrationResult;
import com.example.admin.codegroup.repository.CodeGroupRepository;
import com.example.admin.codegroup.repository.CodeItemRepository;

/**
 * 코드 그룹 관리 서비스.
 *
 * <p>DB에 저장되는 동적 코드 그룹 및 아이템을 관리합니다.
 * 코드 변경 시 {@link CodeGroupChangedEvent}를 발행하여 캐시를 자동 무효화합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeGroupService {

    private final CodeGroupRepository groupRepository;
    private final CodeItemRepository itemRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ========== 그룹 조회 ==========

    /**
     * 소스와 그룹 코드로 그룹 조회.
     */
    @Transactional(readOnly = true)
    public Optional<CodeGroup> findGroup(CodeGroupSource source, String groupCode) {
        return groupRepository.findBySourceAndGroupCode(source, normalizeCode(groupCode));
    }

    /**
     * 그룹 코드로 첫 번째 매칭 그룹 조회 (소스 무관).
     * 
     * <p>소스를 미리 알 수 없는 경우 사용합니다.</p>
     */
    @Transactional(readOnly = true)
    public Optional<CodeGroup> findFirstGroupByCode(String groupCode) {
        return groupRepository.findFirstByGroupCode(normalizeCode(groupCode));
    }

    /**
     * 소스와 그룹 코드로 그룹 조회 (아이템 포함).
     */
    @Transactional(readOnly = true)
    public Optional<CodeGroup> findGroupWithItems(CodeGroupSource source, String groupCode) {
        return groupRepository.findBySourceAndGroupCodeWithItems(source, normalizeCode(groupCode));
    }

    /**
     * ID로 그룹 조회.
     */
    @Transactional(readOnly = true)
    public Optional<CodeGroup> findGroupById(UUID id) {
        return groupRepository.findById(id);
    }

    /**
     * 소스별 모든 그룹 조회.
     */
    @Transactional(readOnly = true)
    public List<CodeGroup> findGroupsBySource(CodeGroupSource source) {
        return groupRepository.findAllBySourceOrderByDisplayOrderAscGroupCodeAsc(source);
    }

    /**
     * 소스별 모든 그룹 조회 (아이템 포함).
     */
    @Transactional(readOnly = true)
    public List<CodeGroup> findGroupsBySourceWithItems(CodeGroupSource source) {
        return groupRepository.findAllBySourceWithItems(source);
    }

    // ========== 아이템 조회 ==========

    /**
     * 소스와 그룹 코드로 활성화된 아이템 목록 조회.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.SYSTEM_COMMON_CODES,
            key = "#source.name() + ':' + #groupCode.toUpperCase()")
    public List<CodeItem> findActiveItems(CodeGroupSource source, String groupCode) {
        return itemRepository.findActiveBySourceAndGroupCode(source, normalizeCode(groupCode));
    }

    /**
     * 소스와 그룹 코드로 모든 아이템 조회.
     */
    @Transactional(readOnly = true)
    public List<CodeItem> findAllItems(CodeGroupSource source, String groupCode) {
        return itemRepository.findBySourceAndGroupCode(source, normalizeCode(groupCode));
    }

    /**
     * 소스, 그룹 코드, 아이템 코드로 아이템 조회.
     */
    @Transactional(readOnly = true)
    public Optional<CodeItem> findItem(CodeGroupSource source, String groupCode, String itemCode) {
        return itemRepository.findBySourceAndGroupCodeAndItemCode(source, normalizeCode(groupCode), itemCode);
    }

    /**
     * ID로 아이템 조회 (그룹 포함).
     */
    @Transactional(readOnly = true)
    public Optional<CodeItem> findItemById(UUID id) {
        return itemRepository.findByIdWithGroup(id);
    }

    // ========== 그룹 CRUD ==========

    /**
     * 동적 코드 그룹 생성.
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.SYSTEM_COMMON_CODES, CacheNames.COMMON_CODE_AGGREGATES}, allEntries = true)
    public CodeGroup createGroup(String groupCode, String groupName, String description, String updatedBy) {
        String normalizedCode = normalizeCode(groupCode);

        // 중복 체크
        if (groupRepository.existsBySourceAndGroupCode(CodeGroupSource.DYNAMIC_DB, normalizedCode)) {
            throw new IllegalArgumentException("이미 존재하는 그룹 코드입니다: " + groupCode);
        }

        CodeGroup group = CodeGroup.createDynamic(
                normalizedCode,
                groupName,
                description,
                updatedBy
        );

        CodeGroup saved = groupRepository.save(group);
        eventPublisher.publishEvent(CodeGroupChangedEvent.groupCreated(this, CodeGroupSource.DYNAMIC_DB, normalizedCode));
        log.debug("Created code group: {}", normalizedCode);

        return saved;
    }

    /**
     * 그룹 정보 수정.
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.SYSTEM_COMMON_CODES, CacheNames.COMMON_CODE_AGGREGATES}, allEntries = true)
    public CodeGroup updateGroup(UUID groupId, String groupName, int displayOrder, boolean active,
                                  String description, String updatedBy) {
        CodeGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹이 존재하지 않습니다."));

        if (!group.isEditable()) {
            throw new IllegalStateException("수정 불가능한 그룹입니다.");
        }

        group.update(groupName, description, active, null, displayOrder, updatedBy, now());
        CodeGroup saved = groupRepository.save(group);

        eventPublisher.publishEvent(CodeGroupChangedEvent.groupUpdated(this, group.getSource(), group.getGroupCode()));
        log.debug("Updated code group: {}/{}", group.getSource(), group.getGroupCode());

        return saved;
    }

    /**
     * 그룹 삭제 (동적 그룹만).
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.SYSTEM_COMMON_CODES, CacheNames.COMMON_CODE_AGGREGATES}, allEntries = true)
    public void deleteGroup(UUID groupId) {
        CodeGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹이 존재하지 않습니다."));

        if (group.getSource() != CodeGroupSource.DYNAMIC_DB) {
            throw new IllegalStateException("동적 그룹만 삭제할 수 있습니다.");
        }

        String groupCode = group.getGroupCode();
        CodeGroupSource source = group.getSource();

        // 아이템 먼저 삭제
        itemRepository.deleteAllByGroupId(groupId);
        groupRepository.delete(group);

        eventPublisher.publishEvent(CodeGroupChangedEvent.groupDeleted(this, source, groupCode));
        log.info("Deleted code group: {}/{}", source, groupCode);
    }

    // ========== 아이템 CRUD ==========

    /**
     * 아이템 생성.
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.SYSTEM_COMMON_CODES, CacheNames.COMMON_CODE_AGGREGATES},
            key = "#source.name() + ':' + #groupCode.toUpperCase()")
    public CodeItem createItem(CodeGroupSource source, String groupCode, String itemCode, String itemName,
                                int displayOrder, boolean active, String description, String metadataJson,
                                String updatedBy) {
        String normalizedGroupCode = normalizeCode(groupCode);

        CodeGroup group = groupRepository.findBySourceAndGroupCode(source, normalizedGroupCode)
                .orElseThrow(() -> new IllegalArgumentException("그룹이 존재하지 않습니다: " + source + "/" + groupCode));

        // 생성 가능 여부 체크 (DYNAMIC_DB만 새 아이템 생성 가능)
        if (!source.isCreatable()) {
            throw new IllegalStateException("해당 소스에서는 새 아이템을 생성할 수 없습니다: " + source);
        }

        // 중복 체크
        if (itemRepository.existsBySourceAndGroupCodeAndItemCode(source, normalizedGroupCode, itemCode)) {
            throw new IllegalArgumentException("이미 존재하는 아이템 코드입니다: " + itemCode);
        }

        CodeItem item = CodeItem.create(group, itemCode, itemName, displayOrder, active,
                description, metadataJson, updatedBy, now());

        CodeItem saved = itemRepository.save(item);
        eventPublisher.publishEvent(CodeGroupChangedEvent.itemCreated(this, source, normalizedGroupCode, itemCode));
        log.debug("Created code item: {}/{}/{}", source, normalizedGroupCode, itemCode);

        return saved;
    }

    /**
     * 아이템 수정.
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.SYSTEM_COMMON_CODES, CacheNames.COMMON_CODE_AGGREGATES}, allEntries = true)
    public CodeItem updateItem(UUID itemId, String itemName, int displayOrder, boolean active,
                                String description, String metadataJson, String updatedBy) {
        CodeItem item = itemRepository.findByIdWithGroup(itemId)
                .orElseThrow(() -> new IllegalArgumentException("아이템이 존재하지 않습니다."));

        if (!item.isEditable()) {
            throw new IllegalStateException("수정 불가능한 아이템입니다.");
        }

        item.update(itemName, displayOrder, active, description, metadataJson, updatedBy, now());
        CodeItem saved = itemRepository.save(item);

        eventPublisher.publishEvent(CodeGroupChangedEvent.itemUpdated(this, item.getSource(), item.getGroupCode(), item.getItemCode()));
        log.debug("Updated code item: {}/{}/{}", item.getSource(), item.getGroupCode(), item.getItemCode());

        return saved;
    }

    /**
     * 아이템 삭제 (통합 API).
     *
     * <p>소스 타입에 따른 삭제 동작:</p>
     * <ul>
     *   <li>STATIC_ENUM: DB 오버라이드 레코드 삭제 → Enum 원본값 복원</li>
     *   <li>DYNAMIC_DB: 영구 삭제</li>
     *   <li>LOCALE_COUNTRY/LOCALE_LANGUAGE (builtIn=true): ISO 원본 복원</li>
     *   <li>LOCALE_COUNTRY/LOCALE_LANGUAGE (builtIn=false): 영구 삭제 (커스텀 항목)</li>
     *   <li>DW, APPROVAL_GROUP: 삭제 불가 (읽기 전용)</li>
     * </ul>
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.SYSTEM_COMMON_CODES, CacheNames.COMMON_CODE_AGGREGATES}, allEntries = true)
    public void deleteItem(UUID itemId) {
        CodeItem item = itemRepository.findByIdWithGroup(itemId)
                .orElseThrow(() -> new IllegalArgumentException("아이템이 존재하지 않습니다."));

        CodeGroupSource source = item.getSource();
        String groupCode = item.getGroupCode();
        String itemCode = item.getItemCode();

        // 삭제 가능 여부 체크
        if (!source.isDeletable()) {
            throw new IllegalStateException("삭제할 수 없는 소스입니다: " + source);
        }

        // 소스별 삭제 동작
        if (source.isRestoreOnDelete() && item.isBuiltIn()) {
            // LOCALE_* builtIn 항목: ISO 원본으로 복원
            restoreToDefault(item);
            log.info("Restored to default: {}/{}/{}", source, groupCode, itemCode);
        } else {
            // STATIC_ENUM 오버라이드, DYNAMIC_DB, LOCALE_* 커스텀: 영구 삭제
            itemRepository.delete(item);
            log.info("Deleted code item: {}/{}/{}", source, groupCode, itemCode);
        }

        eventPublisher.publishEvent(CodeGroupChangedEvent.itemDeleted(this, source, groupCode, itemCode));
    }

    /**
     * Locale 항목을 기본값(ISO 표준)으로 복원.
     */
    private void restoreToDefault(CodeItem item) {
        // ISO 기본값으로 복원 (itemName만 리셋, 나머지 필드 유지)
        // 실제 ISO 기본값은 LocaleCodeInitializer 등에서 관리
        item.update(
                item.getItemCode(),  // ISO 코드를 이름으로 사용 (임시, 추후 ISO 데이터 소스 연동)
                item.getDisplayOrder(),
                true,  // 활성화
                null,  // 설명 초기화
                null,  // 메타데이터 초기화
                "SYSTEM",
                now()
        );
        itemRepository.save(item);
    }

    // ========== Static Enum 오버라이드 ==========

    /**
     * Static Enum 오버라이드용 그룹 생성 또는 조회.
     */
    @Transactional
    public CodeGroup getOrCreateStaticOverrideGroup(String groupCode, String groupName, String updatedBy) {
        String normalizedCode = normalizeCode(groupCode);

        return groupRepository.findBySourceAndGroupCode(CodeGroupSource.STATIC_ENUM, normalizedCode)
                .orElseGet(() -> {
                    CodeGroup group = CodeGroup.createStaticOverride(
                            normalizedCode,
                            groupName,
                            null,
                            updatedBy
                    );
                    return groupRepository.save(group);
                });
    }

    /**
     * Static Enum 아이템 오버라이드 생성 또는 업데이트.
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.SYSTEM_COMMON_CODES, CacheNames.COMMON_CODE_AGGREGATES}, allEntries = true)
    public CodeItem createOrUpdateOverride(String groupCode, String itemCode, String itemName,
                                            Integer displayOrder, String description, String metadataJson,
                                            String updatedBy) {
        String normalizedGroupCode = normalizeCode(groupCode);

        // 그룹 조회 또는 생성
        CodeGroup group = getOrCreateStaticOverrideGroup(normalizedGroupCode, normalizedGroupCode, updatedBy);

        Optional<CodeItem> existingOpt = itemRepository.findBySourceAndGroupCodeAndItemCode(
                CodeGroupSource.STATIC_ENUM, normalizedGroupCode, itemCode);

        CodeItem item;
        if (existingOpt.isPresent()) {
            item = existingOpt.get();
            item.update(
                    itemName != null ? itemName : item.getItemName(),
                    displayOrder != null ? displayOrder : item.getDisplayOrder(),
                    item.isActive(),
                    description != null ? description : item.getDescription(),
                    metadataJson != null ? metadataJson : item.getMetadataJson(),
                    updatedBy,
                    now()
            );
            eventPublisher.publishEvent(CodeGroupChangedEvent.itemUpdated(
                    this, CodeGroupSource.STATIC_ENUM, normalizedGroupCode, itemCode));
        } else {
            item = CodeItem.create(group, itemCode, itemName, displayOrder != null ? displayOrder : 0,
                    true, description, metadataJson, updatedBy, now());
            eventPublisher.publishEvent(CodeGroupChangedEvent.itemCreated(
                    this, CodeGroupSource.STATIC_ENUM, normalizedGroupCode, itemCode));
        }

        CodeItem saved = itemRepository.save(item);
        log.debug("Created/Updated override for {}/{}", normalizedGroupCode, itemCode);
        return saved;
    }

    /**
     * Static Enum 오버라이드 삭제.
     *
     * @deprecated 대신 {@link #deleteItem(UUID)}을 사용하세요.
     *             통합 삭제 API가 소스 타입에 따라 자동으로 적절한 동작을 수행합니다.
     */
    @Deprecated(since = "2025.01", forRemoval = true)
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.SYSTEM_COMMON_CODES, CacheNames.COMMON_CODE_AGGREGATES}, allEntries = true)
    public void deleteOverride(UUID itemId) {
        // 통합 deleteItem으로 위임
        deleteItem(itemId);
    }

    // ========== 마이그레이션 ==========

    /**
     * 그룹 코드 마이그레이션 실행.
     *
     * <p>CodeGroup의 groupCode를 새로운 값으로 일괄 변경합니다.</p>
     *
     * @param groupId 마이그레이션할 CodeGroup의 ID
     * @param newGroupCode 새 그룹 코드
     * @return 마이그레이션 결과 (마이그레이션된 아이템 수, 그룹 ID, 기존/신규 그룹 코드)
     * @throws IllegalArgumentException 그룹을 찾을 수 없거나 DYNAMIC_DB가 아닌 경우
     * @throws IllegalStateException 새 그룹 코드에 이미 레코드가 존재하는 경우
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.SYSTEM_COMMON_CODES, CacheNames.COMMON_CODE_AGGREGATES}, allEntries = true)
    public MigrationResult migrate(UUID groupId, String newGroupCode) {
        String normalizedNew = normalizeCode(newGroupCode);

        // 그룹 조회
        CodeGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹을 찾을 수 없습니다: " + groupId));

        // DYNAMIC_DB만 마이그레이션 가능
        if (group.getSource() != CodeGroupSource.DYNAMIC_DB) {
            throw new IllegalArgumentException("DYNAMIC_DB 소스만 마이그레이션 가능합니다. 현재 소스: " + group.getSource());
        }

        String oldGroupCode = group.getGroupCode();

        // 새 그룹 코드에 이미 레코드가 있는지 확인
        if (groupRepository.existsBySourceAndGroupCode(CodeGroupSource.DYNAMIC_DB, normalizedNew)) {
            throw new IllegalStateException("새 그룹 코드에 이미 레코드가 존재합니다: " + newGroupCode);
        }

        // 그룹 코드 변경
        int itemCount = group.getItems().size();
        group.changeGroupCode(normalizedNew);
        groupRepository.save(group);

        eventPublisher.publishEvent(CodeGroupChangedEvent.migrated(this, CodeGroupSource.DYNAMIC_DB, oldGroupCode, normalizedNew));
        log.info("Migrated {} items from {} to {} (groupId={})", itemCount, oldGroupCode, normalizedNew, groupId);

        return new MigrationResult(itemCount, groupId, oldGroupCode, normalizedNew);
    }

    /**
     * 그룹 코드로 그룹 및 모든 아이템 삭제.
     *
     * <p>Enum에 없는 DB 전용 그룹 코드의 모든 레코드를 삭제합니다.</p>
     *
     * @param groupCode 삭제할 그룹 코드
     * @return 삭제된 아이템 수
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.SYSTEM_COMMON_CODES, CacheNames.COMMON_CODE_AGGREGATES}, allEntries = true)
    public int deleteByGroupCode(String groupCode) {
        String normalizedCode = normalizeCode(groupCode);

        Optional<CodeGroup> groupOpt = groupRepository.findBySourceAndGroupCode(CodeGroupSource.DYNAMIC_DB, normalizedCode);
        if (groupOpt.isEmpty()) {
            return 0;
        }

        CodeGroup group = groupOpt.get();
        int itemCount = group.getItems().size();

        // 아이템 먼저 삭제
        itemRepository.deleteAllByGroupId(group.getId());
        groupRepository.delete(group);

        eventPublisher.publishEvent(CodeGroupChangedEvent.groupDeleted(this, CodeGroupSource.DYNAMIC_DB, normalizedCode));
        log.info("Deleted group and {} items for groupCode: {}", itemCount, normalizedCode);

        return itemCount;
    }

    /**
     * 그룹 코드와 아이템 코드로 아이템 수정.
     *
     * <p>지원되는 소스: DYNAMIC_DB, LOCALE_COUNTRY, LOCALE_LANGUAGE</p>
     *
     * @param source       소스 타입 (nullable이면 그룹에서 추론)
     * @param groupCode    그룹 코드
     * @param itemCode     항목 코드
     * @param itemName     항목명
     * @param displayOrder 표시 순서
     * @param active       활성 상태
     * @param description  설명
     * @param metadataJson 메타데이터
     * @param updatedBy    수정자
     * @return 수정된 아이템
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.SYSTEM_COMMON_CODES, CacheNames.COMMON_CODE_AGGREGATES}, allEntries = true)
    public CodeItem updateItemByGroupAndCode(CodeGroupSource source, String groupCode, String itemCode,
                                              String itemName, Integer displayOrder, boolean active,
                                              String description, String metadataJson, String updatedBy) {
        String normalizedGroupCode = normalizeCode(groupCode);

        // 소스가 지정되지 않으면 그룹에서 추론
        final CodeGroupSource resolvedSource = source != null ? source
                : groupRepository.findFirstByGroupCode(normalizedGroupCode)
                        .map(CodeGroup::getSource)
                        .orElse(CodeGroupSource.DYNAMIC_DB);

        CodeItem item = itemRepository.findBySourceAndGroupCodeAndItemCode(
                resolvedSource, normalizedGroupCode, itemCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "아이템이 존재하지 않습니다: " + resolvedSource + "/" + groupCode + "/" + itemCode));

        if (!item.isEditable()) {
            throw new IllegalStateException("수정 불가능한 아이템입니다.");
        }

        item.update(
                itemName != null ? itemName : item.getItemName(),
                displayOrder != null ? displayOrder : item.getDisplayOrder(),
                active,
                description != null ? description : item.getDescription(),
                metadataJson != null ? metadataJson : item.getMetadataJson(),
                updatedBy,
                now()
        );

        CodeItem saved = itemRepository.save(item);
        eventPublisher.publishEvent(CodeGroupChangedEvent.itemUpdated(
                this, resolvedSource, normalizedGroupCode, itemCode));
        log.debug("Updated code item by group and code: {}/{}/{}", resolvedSource, normalizedGroupCode, itemCode);

        return saved;
    }

    /**
     * 그룹 코드와 아이템 코드로 아이템 수정 (DYNAMIC_DB 기본값).
     *
     * @deprecated 대신 {@link #updateItemByGroupAndCode(CodeGroupSource, String, String, String, Integer, boolean, String, String, String)}을 사용하세요.
     */
    @Deprecated(since = "2025.01", forRemoval = true)
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.SYSTEM_COMMON_CODES, CacheNames.COMMON_CODE_AGGREGATES}, allEntries = true)
    public CodeItem updateItemByGroupAndCode(String groupCode, String itemCode, String itemName,
                                              Integer displayOrder, boolean active, String description,
                                              String metadataJson, String updatedBy) {
        return updateItemByGroupAndCode(null, groupCode, itemCode, itemName, displayOrder,
                active, description, metadataJson, updatedBy);
    }

    // ========== 유틸리티 ==========

    @CacheEvict(cacheNames = CacheNames.SYSTEM_COMMON_CODES, allEntries = true)
    public void evictAll() {
        // eviction only
    }

    private String normalizeCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("code must not be null");
        }
        return code.toUpperCase(Locale.ROOT);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
