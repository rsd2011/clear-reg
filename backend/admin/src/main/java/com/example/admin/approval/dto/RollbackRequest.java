package com.example.admin.approval.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 버전 롤백 요청 DTO.
 */
public record RollbackRequest(
        @NotNull(message = "롤백할 버전 번호는 필수입니다")
        @Min(value = 1, message = "버전 번호는 1 이상이어야 합니다")
        Integer targetVersion,

        @Size(max = 500, message = "변경 사유는 500자 이하여야 합니다")
        String changeReason
) {}
