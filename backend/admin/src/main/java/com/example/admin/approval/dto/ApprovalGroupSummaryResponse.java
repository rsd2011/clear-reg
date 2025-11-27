package com.example.admin.approval.dto;

import java.util.UUID;
import java.util.function.UnaryOperator;

import com.example.admin.approval.ApprovalGroup;

/**
 * 승인그룹 요약 정보 응답 DTO.
 * 템플릿 생성/수정 시 승인그룹 매핑에 사용됩니다.
 */
public record ApprovalGroupSummaryResponse(
        UUID id,
        String groupCode,
        String name
) {
    public static ApprovalGroupSummaryResponse from(ApprovalGroup group) {
        return from(group, UnaryOperator.identity());
    }

    public static ApprovalGroupSummaryResponse from(ApprovalGroup group, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new ApprovalGroupSummaryResponse(
                group.getId(),
                fn.apply(group.getGroupCode()),
                fn.apply(group.getName())
        );
    }

    public static ApprovalGroupSummaryResponse apply(ApprovalGroupSummaryResponse response, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new ApprovalGroupSummaryResponse(
                response.id(),
                fn.apply(response.groupCode()),
                fn.apply(response.name())
        );
    }
}
