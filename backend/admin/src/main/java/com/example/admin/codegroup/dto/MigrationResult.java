package com.example.admin.codegroup.dto;

import java.util.UUID;

/**
 * 마이그레이션 실행 결과 내부 DTO.
 *
 * <p>Service에서 Controller로 마이그레이션 결과를 전달할 때 사용합니다.</p>
 *
 * @param migratedCount 마이그레이션된 코드 수
 * @param groupId       마이그레이션된 그룹 ID
 * @param oldGroupCode  기존 그룹 코드
 * @param newGroupCode  새 그룹 코드
 */
public record MigrationResult(
        int migratedCount,
        UUID groupId,
        String oldGroupCode,
        String newGroupCode
) {}
