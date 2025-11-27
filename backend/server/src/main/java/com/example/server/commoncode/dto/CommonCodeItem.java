package com.example.server.commoncode.dto;

import com.example.server.commoncode.model.CommonCodeKind;
import com.example.server.commoncode.model.CommonCodeSource;

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
