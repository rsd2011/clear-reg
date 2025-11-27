package com.example.admin.codemanage.model;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

import com.example.common.jpa.PrimaryKeyEntity;
import lombok.Getter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "system_common_codes",
        indexes = @Index(name = "idx_system_common_code_type_order", columnList = "code_type, display_order, code_value"))
@Getter
public class SystemCommonCode extends PrimaryKeyEntity {

    protected SystemCommonCode() {
    }

    private SystemCommonCode(String codeType,
                             String codeValue,
                             String codeName,
                             int displayOrder,
                             CodeManageKind codeKind,
                             boolean active,
                             String description,
                             String metadataJson,
                             String updatedBy,
                             OffsetDateTime updatedAt) {
        this.codeType = codeType;
        this.codeValue = codeValue;
        this.codeName = codeName;
        this.displayOrder = displayOrder;
        this.codeKind = codeKind == null ? CodeManageKind.DYNAMIC : codeKind;
        this.active = active;
        this.description = description;
        this.metadataJson = metadataJson;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt == null ? OffsetDateTime.now(ZoneOffset.UTC) : updatedAt;
    }

    public static SystemCommonCode create(String codeType,
                                          String codeValue,
                                          String codeName,
                                          int displayOrder,
                                          CodeManageKind codeKind,
                                          boolean active,
                                          String description,
                                          String metadataJson,
                                          String updatedBy,
                                          OffsetDateTime updatedAt) {
        return new SystemCommonCode(normalize(codeType), codeValue, codeName, displayOrder,
                codeKind, active, description, metadataJson, updatedBy, updatedAt);
    }

    @Column(name = "code_type", nullable = false, length = 64)
    private String codeType;

    @Column(name = "code_value", nullable = false, length = 128)
    private String codeValue;

    @Column(name = "code_name", nullable = false, length = 255)
    private String codeName;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "code_kind", nullable = false, length = 16)
    private CodeManageKind codeKind = CodeManageKind.DYNAMIC;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadataJson;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Column(name = "updated_by", nullable = false, length = 128)
    private String updatedBy;

    public void update(String codeName,
                       int displayOrder,
                       CodeManageKind codeKind,
                       boolean active,
                       String description,
                       String metadataJson,
                       String updatedBy,
                       OffsetDateTime updatedAt) {
        enforceKind(codeKind, this.codeType);
        this.codeName = codeName;
        this.displayOrder = displayOrder;
        this.codeKind = codeKind == null ? this.codeKind : codeKind;
        this.active = active;
        this.description = description;
        this.metadataJson = metadataJson;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt == null ? OffsetDateTime.now(ZoneOffset.UTC) : updatedAt;
    }

    public SystemCommonCode copy() {
        return new SystemCommonCode(codeType, codeValue, codeName, displayOrder, codeKind, active,
                description, metadataJson, updatedBy, updatedAt);
    }

    private static void enforceKind(CodeManageKind requestedKind, String codeType) {
        SystemCommonCodeType.fromCode(codeType).ifPresent(type -> {
            CodeManageKind expected = type.defaultKind();
            if (requestedKind != null && requestedKind != expected) {
                throw new IllegalArgumentException("코드 타입 " + codeType + "는 kind=" + expected + "만 허용");
            }
        });
    }

    private static String normalize(String codeType) {
        return codeType == null ? null : codeType.toUpperCase(Locale.ROOT);
    }
}
