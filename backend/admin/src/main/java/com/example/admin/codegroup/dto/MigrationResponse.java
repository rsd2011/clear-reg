package com.example.admin.codegroup.dto;

import java.util.UUID;

/**
 * 마이그레이션 실행 결과 응답 DTO.
 *
 * @param success        성공 여부
 * @param migratedCount  마이그레이션된 코드 수
 * @param groupId        마이그레이션된 그룹 ID
 * @param oldGroupCode   기존 그룹 코드
 * @param newGroupCode   새 그룹 코드
 * @param message        결과 메시지
 */
public record MigrationResponse(
        boolean success,
        int migratedCount,
        UUID groupId,
        String oldGroupCode,
        String newGroupCode,
        String message
) {

    /**
     * 성공 응답 생성
     */
    public static MigrationResponse success(int migratedCount, UUID groupId, String oldGroupCode, String newGroupCode) {
        String message = String.format("%d개 코드가 %s에서 %s로 마이그레이션되었습니다.",
                migratedCount, oldGroupCode, newGroupCode);
        return new MigrationResponse(true, migratedCount, groupId, oldGroupCode, newGroupCode, message);
    }

    /**
     * 실패 응답 생성
     */
    public static MigrationResponse failure(UUID groupId, String newGroupCode, String errorMessage) {
        return new MigrationResponse(false, 0, groupId, null, newGroupCode, errorMessage);
    }
}
