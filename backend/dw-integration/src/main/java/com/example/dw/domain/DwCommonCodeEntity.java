package com.example.dw.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.common.jpa.PrimaryKeyEntity;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "dw_common_codes",
        indexes = {
                @Index(name = "idx_dw_common_code_type_order", columnList = "code_type, display_order, code_value")
        })
@Getter
@Setter
public class DwCommonCodeEntity extends PrimaryKeyEntity {

    @Column(name = "code_type", nullable = false, length = 64)
    private String codeType;

    @Column(name = "code_value", nullable = false, length = 128)
    private String codeValue;

    @Column(name = "code_name", nullable = false, length = 255)
    private String codeName;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "category", length = 128)
    private String category;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadataJson;

    @Column(name = "synced_at", nullable = false)
    private OffsetDateTime syncedAt;

    @Column(name = "source_batch_id", columnDefinition = "uuid", nullable = false)
    private UUID sourceBatchId;

    public void setCodeType(String codeType) {
        this.codeType = codeType == null ? null : codeType.toUpperCase(java.util.Locale.ROOT);
    }

    public boolean sameBusinessState(String codeName,
                                     int displayOrder,
                                     boolean active,
                                     String category,
                                     String description,
                                     String metadataJson) {
        return this.displayOrder == displayOrder
                && this.active == active
                && safeEquals(this.codeName, codeName)
                && safeEquals(this.category, category)
                && safeEquals(this.description, description)
                && safeEquals(this.metadataJson, metadataJson);
    }

    private static boolean safeEquals(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }
}
