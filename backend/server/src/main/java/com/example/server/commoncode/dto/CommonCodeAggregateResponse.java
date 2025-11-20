package com.example.server.commoncode.dto;

import java.util.List;

import com.example.server.commoncode.CommonCodeItem;

public record CommonCodeAggregateResponse(String codeType,
                                          List<CommonCodeItemResponse> items) {

    public static CommonCodeAggregateResponse of(String codeType, List<CommonCodeItem> items) {
        return new CommonCodeAggregateResponse(codeType,
                items.stream().map(CommonCodeItemResponse::from).toList());
    }
}
