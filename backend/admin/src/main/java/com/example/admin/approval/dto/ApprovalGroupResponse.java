package com.example.admin.approval.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.UnaryOperator;

import com.example.admin.approval.ApprovalGroup;

public record ApprovalGroupResponse(
        UUID id,
        String groupCode,
        String name,
        String description,
        Integer displayOrder,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ApprovalGroupResponse from(ApprovalGroup group) {
        return from(group, UnaryOperator.identity());
    }

    public static ApprovalGroupResponse from(ApprovalGroup group, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new ApprovalGroupResponse(
                group.getId(),
                fn.apply(group.getGroupCode()),
                fn.apply(group.getName()),
                fn.apply(group.getDescription()),
                group.getDisplayOrder(),
                group.isActive(),
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }

    public static ApprovalGroupResponse apply(ApprovalGroupResponse response, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new ApprovalGroupResponse(
                response.id(),
                fn.apply(response.groupCode()),
                fn.apply(response.name()),
                fn.apply(response.description()),
                response.displayOrder(),
                response.active(),
                response.createdAt(),
                response.updatedAt());
    }
}
