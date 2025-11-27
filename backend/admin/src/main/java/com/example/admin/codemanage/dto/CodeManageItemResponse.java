package com.example.admin.codemanage.dto;

import com.example.admin.codemanage.model.CodeManageKind;
import com.example.admin.codemanage.model.CodeManageSource;
import java.util.function.UnaryOperator;

public record CodeManageItemResponse(String codeValue,
                                     String codeName,
                                     Integer displayOrder,
                                     CodeManageKind codeKind,
                                     CodeManageSource source,
                                     boolean active,
                                     String description,
                                     String metadataJson) {

    public static CodeManageItemResponse from(CodeManageItem item) {
        return from(item, UnaryOperator.identity());
    }

    public static CodeManageItemResponse from(CodeManageItem item, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new CodeManageItemResponse(fn.apply(item.codeValue()),
                fn.apply(item.codeName()),
                item.displayOrder(),
                item.codeKind(),
                item.source(),
                item.active(),
                fn.apply(item.description()),
                fn.apply(item.metadataJson()));
    }
}
