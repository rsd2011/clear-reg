package com.example.admin.draft.dto;

import jakarta.validation.constraints.Size;

/**
 * 롤백 요청 DTO.
 *
 * @param changeReason   롤백 사유
 * @param overwriteDraft 기존 초안 덮어쓰기 여부
 */
public record RollbackRequest(
        @Size(max = 500) String changeReason,
        boolean overwriteDraft
) {
    public RollbackRequest {
        if (changeReason == null) {
            changeReason = "";
        }
    }
}
