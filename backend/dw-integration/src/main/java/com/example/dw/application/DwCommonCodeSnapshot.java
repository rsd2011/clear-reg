package com.example.dw.application;

import com.example.dw.domain.DwCommonCodeEntity;

public record DwCommonCodeSnapshot(String codeType,
                                   String codeValue,
                                   String codeName,
                                   int displayOrder,
                                   boolean active,
                                   String category,
                                   String description,
                                   String metadataJson) {

    public static DwCommonCodeSnapshot fromEntity(DwCommonCodeEntity entity) {
        return new DwCommonCodeSnapshot(entity.getCodeType(),
                entity.getCodeValue(),
                entity.getCodeName(),
                entity.getDisplayOrder(),
                entity.isActive(),
                entity.getCategory(),
                entity.getDescription(),
                entity.getMetadataJson());
    }
}
