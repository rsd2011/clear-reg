package com.example.admin.codemanage.dto;

import org.hibernate.validator.constraints.Length;

import com.example.admin.codemanage.model.CommonCodeKind;
import com.example.admin.codemanage.model.SystemCommonCode;

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
            return SystemCommonCode.create(
                    null, // codeType은 서비스에서 주입
                    codeValue.trim(),
                    codeName.trim(),
                    displayOrder == null ? 0 : displayOrder,
                    codeKind,
                    active,
                    description,
                    metadataJson,
                    username,
                    null
            );
        }
    }
