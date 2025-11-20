package com.example.server.commoncode.dto;

import org.hibernate.validator.constraints.Length;

import com.example.server.commoncode.model.CommonCodeKind;
import com.example.server.commoncode.model.SystemCommonCode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SystemCommonCodeRequest(
        @NotBlank String codeValue,
        @NotBlank String codeName,
        Integer displayOrder,
        CommonCodeKind codeKind,
        boolean active,
        @Size(max = 512) String description,
        @Length(max = 2000) String metadataJson) {

    public SystemCommonCode toEntity(String username) {
        SystemCommonCode entity = new SystemCommonCode();
        entity.setCodeValue(codeValue.trim());
        entity.setCodeName(codeName.trim());
        entity.setDisplayOrder(displayOrder == null ? 0 : displayOrder);
        entity.setCodeKind(codeKind);
        entity.setActive(active);
        entity.setDescription(description);
        entity.setMetadataJson(metadataJson);
        entity.setUpdatedBy(username);
        return entity;
    }
}
