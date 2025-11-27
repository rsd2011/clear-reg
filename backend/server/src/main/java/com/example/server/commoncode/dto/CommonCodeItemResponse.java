package com.example.server.commoncode.dto;

import com.example.server.commoncode.model.CommonCodeKind;
import com.example.server.commoncode.model.CommonCodeSource;
import java.util.function.UnaryOperator;

public record CommonCodeItemResponse(String codeValue,
                                     String codeName,
                                     Integer displayOrder,
                                     CommonCodeKind codeKind,
                                     CommonCodeSource source,
                                     boolean active,
                                     String description,
                                     String metadataJson) {

    public static CommonCodeItemResponse from(CommonCodeItem item) {
        return from(item, UnaryOperator.identity());
    }

    public static CommonCodeItemResponse from(CommonCodeItem item, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new CommonCodeItemResponse(fn.apply(item.codeValue()),
                fn.apply(item.codeName()),
                item.displayOrder(),
                item.codeKind(),
                item.source(),
                item.active(),
                fn.apply(item.description()),
                fn.apply(item.metadataJson()));
    }
}
