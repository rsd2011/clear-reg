package com.example.server.commoncode.dto;

import java.util.List;
import java.util.function.UnaryOperator;

public record CommonCodeAggregateResponse(String codeType,
                                          List<CommonCodeItemResponse> items) {

    public static CommonCodeAggregateResponse of(String codeType, List<CommonCodeItem> items) {
        return of(codeType, items, UnaryOperator.identity());
    }

    public static CommonCodeAggregateResponse of(String codeType, List<CommonCodeItem> items, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new CommonCodeAggregateResponse(codeType,
                items.stream().map(i -> CommonCodeItemResponse.from(i, fn)).toList());
    }
}
