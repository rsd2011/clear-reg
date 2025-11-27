package com.example.admin.codemanage.dto;

import java.util.List;
import java.util.function.UnaryOperator;

public record CodeManageAggregateResponse(String codeType,
                                          List<CodeManageItemResponse> items) {

    public static CodeManageAggregateResponse of(String codeType, List<CodeManageItem> items) {
        return of(codeType, items, UnaryOperator.identity());
    }

    public static CodeManageAggregateResponse of(String codeType, List<CodeManageItem> items, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new CodeManageAggregateResponse(codeType,
                items.stream().map(i -> CodeManageItemResponse.from(i, fn)).toList());
    }
}
