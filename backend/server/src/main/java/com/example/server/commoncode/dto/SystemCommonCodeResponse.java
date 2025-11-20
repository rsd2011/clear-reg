package com.example.server.commoncode.dto;

import java.time.OffsetDateTime;

import com.example.server.commoncode.model.CommonCodeKind;
import com.example.server.commoncode.model.SystemCommonCode;

public record SystemCommonCodeResponse(String codeType,
                                       String codeValue,
                                       String codeName,
                                       int displayOrder,
                                       CommonCodeKind codeKind,
                                       boolean active,
                                       String description,
                                       String metadataJson,
                                       OffsetDateTime updatedAt,
                                       String updatedBy) {

    public static SystemCommonCodeResponse from(SystemCommonCode code) {
        return new SystemCommonCodeResponse(code.getCodeType(),
                code.getCodeValue(),
                code.getCodeName(),
                code.getDisplayOrder(),
                code.getCodeKind(),
                code.isActive(),
                code.getDescription(),
                code.getMetadataJson(),
                code.getUpdatedAt(),
                code.getUpdatedBy());
    }
}
