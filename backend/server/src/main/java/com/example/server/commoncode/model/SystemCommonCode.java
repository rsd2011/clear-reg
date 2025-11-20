package com.example.server.commoncode.model;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

import com.example.common.jpa.PrimaryKeyEntity;
import lombok.Getter;
import lombok.Setter;

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
@Setter
public class SystemCommonCode extends PrimaryKeyEntity {

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
    private CommonCodeKind codeKind = CommonCodeKind.DYNAMIC;

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

    public void setCodeType(String codeType) {
        this.codeType = codeType == null ? null : codeType.toUpperCase(Locale.ROOT);
    }

    public void updateFrom(SystemCommonCode update) {
        this.codeName = update.codeName;
        this.displayOrder = update.displayOrder;
        this.codeKind = update.codeKind;
        this.active = update.active;
        this.description = update.description;
        this.metadataJson = update.metadataJson;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.updatedBy = update.updatedBy;
    }

    public static SystemCommonCode of(String codeType, SystemCommonCode code) {
        SystemCommonCode entity = new SystemCommonCode();
        entity.codeType = codeType;
        entity.codeValue = code.codeValue;
        entity.codeName = code.codeName;
        entity.displayOrder = code.displayOrder;
        entity.codeKind = code.codeKind == null ? CommonCodeKind.DYNAMIC : code.codeKind;
        entity.active = code.active;
        entity.description = code.description;
        entity.metadataJson = code.metadataJson;
        entity.updatedBy = code.updatedBy;
        entity.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
        return entity;
    }

    public SystemCommonCode copy() {
        SystemCommonCode clone = new SystemCommonCode();
        clone.codeType = this.codeType;
        clone.codeValue = this.codeValue;
        clone.codeName = this.codeName;
        clone.displayOrder = this.displayOrder;
        clone.codeKind = this.codeKind;
        clone.active = this.active;
        clone.description = this.description;
        clone.metadataJson = this.metadataJson;
        clone.updatedAt = this.updatedAt;
        clone.updatedBy = this.updatedBy;
        return clone;
    }
}
