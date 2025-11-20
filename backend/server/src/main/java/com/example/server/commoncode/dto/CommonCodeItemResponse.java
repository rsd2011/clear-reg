package com.example.server.commoncode.dto;

import com.example.server.commoncode.CommonCodeItem;
import com.example.server.commoncode.model.CommonCodeKind;
import com.example.server.commoncode.model.CommonCodeSource;

public record CommonCodeItemResponse(String codeValue,
                                     String codeName,
                                     Integer displayOrder,
                                     CommonCodeKind codeKind,
                                     CommonCodeSource source,
                                     boolean active,
                                     String description,
                                     String metadataJson) {

    public static CommonCodeItemResponse from(CommonCodeItem item) {
        return new CommonCodeItemResponse(item.codeValue(),
                item.codeName(),
                item.displayOrder(),
                item.codeKind(),
                item.source(),
                item.active(),
                item.description(),
                item.metadataJson());
    }
}
