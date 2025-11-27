package com.example.admin.approval.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * 승인선 템플릿 표시순서 일괄 변경 요청 DTO.
 */
public record DisplayOrderUpdateRequest(
        @NotEmpty(message = "변경할 항목 목록이 비어있습니다")
        @Valid
        List<DisplayOrderItem> items
) {
    /**
     * 개별 템플릿의 표시순서 변경 항목.
     */
    public record DisplayOrderItem(
            @NotNull(message = "템플릿 ID는 필수입니다")
            UUID id,

            @NotNull(message = "표시순서는 필수입니다")
            @Min(value = 0, message = "표시순서는 0 이상이어야 합니다")
            Integer displayOrder
    ) {
    }
}
