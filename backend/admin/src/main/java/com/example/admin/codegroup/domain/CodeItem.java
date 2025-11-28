package com.example.admin.codegroup.domain;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.example.common.jpa.PrimaryKeyEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 코드 아이템 엔티티.
 *
 * <p>CodeGroup에 속하는 개별 코드 항목을 나타냅니다.</p>
 * <p>유니크 제약: group_id + item_code</p>
 */
@Entity
@Table(name = "code_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_code_item_group_code",
                columnNames = {"group_id", "item_code"}
        ),
        indexes = {
                @Index(name = "idx_code_item_group", columnList = "group_id, display_order"),
                @Index(name = "idx_code_item_code", columnList = "item_code")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CodeItem extends PrimaryKeyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private CodeGroup codeGroup;

    @Column(name = "item_code", nullable = false, length = 128)
    private String itemCode;

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadataJson;

    /**
     * ISO 표준 제공 항목 여부 (Locale 소스용).
     * <ul>
     *   <li>true: ISO 표준 항목 (삭제 시 원복, 오버라이드만 가능)</li>
     *   <li>false: 커스텀 추가 항목 (전체 CRUD 가능) 또는 일반 항목</li>
     * </ul>
     */
    @Column(name = "built_in", nullable = false)
    private boolean builtIn = false;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Column(name = "updated_by", nullable = false, length = 128)
    private String updatedBy;

    private CodeItem(CodeGroup codeGroup,
                     String itemCode,
                     String itemName,
                     int displayOrder,
                     boolean active,
                     String description,
                     String metadataJson,
                     boolean builtIn,
                     String updatedBy,
                     OffsetDateTime updatedAt) {
        this.codeGroup = codeGroup;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.displayOrder = displayOrder;
        this.active = active;
        this.description = description;
        this.metadataJson = metadataJson;
        this.builtIn = builtIn;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt == null ? OffsetDateTime.now(ZoneOffset.UTC) : updatedAt;
    }

    /**
     * 새 코드 아이템 생성 (일반용, builtIn=false)
     */
    public static CodeItem create(CodeGroup codeGroup,
                                  String itemCode,
                                  String itemName,
                                  int displayOrder,
                                  boolean active,
                                  String description,
                                  String metadataJson,
                                  String updatedBy,
                                  OffsetDateTime updatedAt) {
        return new CodeItem(codeGroup, itemCode, itemName, displayOrder,
                active, description, metadataJson, false, updatedBy, updatedAt);
    }

    /**
     * Locale ISO 항목 생성 (builtIn 지정 가능)
     */
    public static CodeItem createLocaleItem(CodeGroup codeGroup,
                                            String itemCode,
                                            String itemName,
                                            int displayOrder,
                                            String metadataJson,
                                            boolean builtIn,
                                            String updatedBy) {
        return new CodeItem(codeGroup, itemCode, itemName, displayOrder,
                true, null, metadataJson, builtIn, updatedBy, null);
    }

    /**
     * 아이템 정보 수정
     */
    public void update(String itemName,
                       int displayOrder,
                       boolean active,
                       String description,
                       String metadataJson,
                       String updatedBy,
                       OffsetDateTime updatedAt) {
        this.itemName = itemName;
        this.displayOrder = displayOrder;
        this.active = active;
        this.description = description;
        this.metadataJson = metadataJson;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt == null ? OffsetDateTime.now(ZoneOffset.UTC) : updatedAt;
    }

    /**
     * 그룹 코드 반환 (편의 메서드)
     */
    public String getGroupCode() {
        return codeGroup != null ? codeGroup.getGroupCode() : null;
    }

    /**
     * 소스 타입 반환 (편의 메서드)
     */
    public CodeGroupSource getSource() {
        return codeGroup != null ? codeGroup.getSource() : null;
    }

    /**
     * 수정 가능한지 확인
     */
    public boolean isEditable() {
        return codeGroup != null && codeGroup.isEditable();
    }

    /**
     * 삭제 가능한지 확인 (enum 위임)
     */
    public boolean isDeletable() {
        return codeGroup != null && codeGroup.getSource().isDeletable();
    }

    /**
     * 복사본 생성 (같은 그룹 내)
     */
    public CodeItem copy() {
        return new CodeItem(codeGroup, itemCode, itemName, displayOrder,
                active, description, metadataJson, builtIn, updatedBy, updatedAt);
    }

    /**
     * 아이템 이름만 수정 (Locale 오버라이드용)
     */
    public void updateName(String itemName, String updatedBy) {
        this.itemName = itemName;
        this.updatedBy = updatedBy;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
