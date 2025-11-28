package com.example.admin.codegroup.dto;

import java.util.List;

/**
 * 마이그레이션 상태 응답 DTO.
 *
 * <p>Enum과 DB 간의 groupCode 불일치 상태를 표현합니다.</p>
 *
 * @param enumOnlyGroups Enum에만 존재하고 DB에 레코드가 없는 그룹들
 * @param dbOnlyGroups   DB에만 존재하고 Enum에 없는 그룹들 (마이그레이션 대상)
 * @param syncedGroups   Enum과 DB가 동기화된 그룹들
 */
public record MigrationStatusResponse(
        List<CodeGroupStatus> enumOnlyGroups,
        List<CodeGroupStatus> dbOnlyGroups,
        List<SyncedGroupStatus> syncedGroups
) {

    /**
     * 코드 그룹 상태 DTO.
     *
     * @param groupCode   그룹 코드명
     * @param itemCount   코드 항목 수
     * @param description 설명
     */
    public record CodeGroupStatus(
            String groupCode,
            int itemCount,
            String description
    ) {
        public static CodeGroupStatus of(String groupCode, int itemCount, String description) {
            return new CodeGroupStatus(groupCode, itemCount, description);
        }
    }

    /**
     * 동기화된 코드 그룹 상태 DTO.
     *
     * @param groupCode       그룹 코드명
     * @param enumCount       Enum에 정의된 코드 수
     * @param dbOverrideCount DB 오버라이드 레코드 수
     * @param synced          완전 동기화 여부
     */
    public record SyncedGroupStatus(
            String groupCode,
            int enumCount,
            int dbOverrideCount,
            boolean synced
    ) {
        public static SyncedGroupStatus of(String groupCode, int enumCount, int dbOverrideCount) {
            return new SyncedGroupStatus(groupCode, enumCount, dbOverrideCount, true);
        }
    }
}
