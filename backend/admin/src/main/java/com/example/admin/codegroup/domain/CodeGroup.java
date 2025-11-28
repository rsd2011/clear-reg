package com.example.admin.codegroup.domain;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.common.jpa.PrimaryKeyEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 코드 그룹 엔티티.
 *
 * <p>코드 항목(CodeItem)들을 그룹화하여 관리합니다.</p>
 * <p>유니크 제약: source + groupCode</p>
 */
@Entity
@Table(name = "code_groups",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_code_group_source_code",
                columnNames = {"source", "group_code"}
        ),
        indexes = {
                @Index(name = "idx_code_group_code", columnList = "group_code"),
                @Index(name = "idx_code_group_source", columnList = "source")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CodeGroup extends PrimaryKeyEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 32)
    private CodeGroupSource source;

    @Column(name = "group_code", nullable = false, length = 64)
    private String groupCode;

    @Column(name = "group_name", nullable = false, length = 255)
    private String groupName;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadataJson;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Column(name = "updated_by", nullable = false, length = 128)
    private String updatedBy;

    @OneToMany(mappedBy = "codeGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, itemCode ASC")
    private List<CodeItem> items = new ArrayList<>();

    private CodeGroup(CodeGroupSource source,
                      String groupCode,
                      String groupName,
                      String description,
                      boolean active,
                      String metadataJson,
                      int displayOrder,
                      String updatedBy,
                      OffsetDateTime updatedAt) {
        this.source = source;
        this.groupCode = normalize(groupCode);
        this.groupName = groupName;
        this.description = description;
        this.active = active;
        this.metadataJson = metadataJson;
        this.displayOrder = displayOrder;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt == null ? OffsetDateTime.now(ZoneOffset.UTC) : updatedAt;
    }

    /**
     * 새 코드 그룹 생성
     */
    public static CodeGroup create(CodeGroupSource source,
                                   String groupCode,
                                   String groupName,
                                   String description,
                                   boolean active,
                                   String metadataJson,
                                   int displayOrder,
                                   String updatedBy,
                                   OffsetDateTime updatedAt) {
        return new CodeGroup(source, groupCode, groupName, description,
                active, metadataJson, displayOrder, updatedBy, updatedAt);
    }

    /**
     * 동적 코드 그룹 간편 생성
     */
    public static CodeGroup createDynamic(String groupCode,
                                          String groupName,
                                          String description,
                                          String updatedBy) {
        return new CodeGroup(CodeGroupSource.DYNAMIC_DB, groupCode, groupName, description,
                true, null, 0, updatedBy, null);
    }

    /**
     * 정적 Enum 오버라이드용 그룹 생성
     */
    public static CodeGroup createStaticOverride(String groupCode,
                                                 String groupName,
                                                 String description,
                                                 String updatedBy) {
        return new CodeGroup(CodeGroupSource.STATIC_ENUM, groupCode, groupName, description,
                true, null, 0, updatedBy, null);
    }

    /**
     * 그룹 정보 수정
     */
    public void update(String groupName,
                       String description,
                       boolean active,
                       String metadataJson,
                       int displayOrder,
                       String updatedBy,
                       OffsetDateTime updatedAt) {
        this.groupName = groupName;
        this.description = description;
        this.active = active;
        this.metadataJson = metadataJson;
        this.displayOrder = displayOrder;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt == null ? OffsetDateTime.now(ZoneOffset.UTC) : updatedAt;
    }

    /**
     * 코드 아이템 추가
     */
    public CodeItem addItem(String itemCode,
                            String itemName,
                            int displayOrder,
                            boolean active,
                            String description,
                            String metadataJson,
                            String updatedBy) {
        CodeItem item = CodeItem.create(this, itemCode, itemName, displayOrder,
                active, description, metadataJson, updatedBy, null);
        this.items.add(item);
        return item;
    }

    /**
     * 코드 아이템 제거
     */
    public boolean removeItem(CodeItem item) {
        return this.items.remove(item);
    }

    /**
     * 아이템 코드로 조회
     */
    public CodeItem findItem(String itemCode) {
        return this.items.stream()
                .filter(item -> item.getItemCode().equals(itemCode))
                .findFirst()
                .orElse(null);
    }

    /**
     * 수정 가능한 소스인지 확인
     */
    public boolean isEditable() {
        return this.source.isEditable();
    }

    /**
     * 정적 Enum 기반 그룹인지 확인 (source 위임)
     */
    public boolean isStatic() {
        return this.source.isStaticEnum();
    }

    /**
     * 동적 DB 관리 그룹인지 확인 (source 위임)
     */
    public boolean isDynamic() {
        return this.source.isDynamic();
    }

    /**
     * 복사본 생성
     */
    public CodeGroup copy() {
        return new CodeGroup(source, groupCode, groupName, description,
                active, metadataJson, displayOrder, updatedBy, updatedAt);
    }

    /**
     * 그룹 코드 변경 (마이그레이션용)
     */
    public void changeGroupCode(String newGroupCode) {
        this.groupCode = normalize(newGroupCode);
    }

    private static String normalize(String groupCode) {
        return groupCode == null ? null : groupCode.toUpperCase(Locale.ROOT);
    }
}
