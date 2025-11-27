package com.example.admin.codemanage.dto;

import com.example.admin.codemanage.model.CommonCodeKind;
import com.example.admin.codemanage.model.CommonCodeSource;

public record CommonCodeItem(String codeType,
                             String codeValue,
                             String codeName,
                             Integer displayOrder,
                             boolean active,
                             CommonCodeKind codeKind,
                             CommonCodeSource source,
                             String description,
                             String metadataJson) {
}
