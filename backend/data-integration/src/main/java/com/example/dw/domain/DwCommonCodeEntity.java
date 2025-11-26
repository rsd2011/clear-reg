package com.example.dw.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.common.jpa.PrimaryKeyEntity;
import lombok.Getter;

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
public class DwCommonCodeEntity extends PrimaryKeyEntity {

    protected DwCommonCodeEntity() {
    }

    private DwCommonCodeEntity(String codeType,
                               String codeValue,
                               String codeName,
                               int displayOrder,
                               boolean active,
                               String category,
                               String description,
                               String metadataJson,
                               UUID sourceBatchId,
                               OffsetDateTime syncedAt) {
        this.codeType = codeType;
        this.codeValue = codeValue;
        this.codeName = codeName;
        this.displayOrder = displayOrder;
        this.active = active;
        this.category = category;
        this.description = description;
        this.metadataJson = metadataJson;
        this.sourceBatchId = sourceBatchId;
        this.syncedAt = syncedAt;
    }

    public static DwCommonCodeEntity create(String codeType,
                                            String codeValue,
                                            String codeName,
                                            Integer displayOrder,
                                            boolean active,
                                            String category,
                                            String description,
                                            String metadataJson,
                                            UUID sourceBatchId,
                                            OffsetDateTime syncedAt) {
        return new DwCommonCodeEntity(normalize(codeType), codeValue, codeName,
                displayOrder == null ? 0 : displayOrder, active, category, description, metadataJson, sourceBatchId, syncedAt);
    }

    public void updateFromRecord(String codeName,
                                 Integer displayOrder,
                                 boolean active,
                                 String category,
                                 String description,
                                 String metadataJson,
                                 UUID sourceBatchId,
                                 OffsetDateTime syncedAt) {
        this.codeName = codeName;
        this.displayOrder = displayOrder == null ? 0 : displayOrder;
        this.active = active;
        this.category = category;
        this.description = description;
        this.metadataJson = metadataJson;
        this.sourceBatchId = sourceBatchId;
        this.syncedAt = syncedAt;
    }

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

    private static String normalize(String codeType) {
        return codeType == null ? null : codeType.toUpperCase(java.util.Locale.ROOT);
    }
}
