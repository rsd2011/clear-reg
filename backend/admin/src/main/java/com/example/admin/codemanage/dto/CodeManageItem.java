package com.example.admin.codemanage.dto;

import com.example.admin.codemanage.model.CodeManageKind;
import com.example.admin.codemanage.model.CodeManageSource;

public record CodeManageItem(String codeType,
                             String codeValue,
                             String codeName,
                             Integer displayOrder,
                             boolean active,
                             CodeManageKind codeKind,
                             CodeManageSource source,
                             String description,
                             String metadataJson) {
}
