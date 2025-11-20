package com.example.draft.application.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.draft.domain.ApprovalGroup;

public record ApprovalGroupResponse(
        UUID id,
        String groupCode,
        String name,
        String description,
        String organizationCode,
        String conditionExpression,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ApprovalGroupResponse from(ApprovalGroup group) {
        return new ApprovalGroupResponse(
                group.getId(),
                group.getGroupCode(),
                group.getName(),
                group.getDescription(),
                group.getOrganizationCode(),
                group.getConditionExpression(),
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }
}
