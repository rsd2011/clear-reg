package com.example.common.version;

/**
 * 필드 변경 정보.
 * 버전 비교 시 단일 필드의 변경 내용을 나타냅니다.
 *
 * @param fieldName   필드명 (코드상의 이름)
 * @param fieldLabel  필드 라벨 (UI 표시용)
 * @param beforeValue 변경 전 값
 * @param afterValue  변경 후 값
 * @param diffType    변경 유형
 */
public record FieldDiff(
        String fieldName,
        String fieldLabel,
        Object beforeValue,
        Object afterValue,
        DiffType diffType
) {
    /**
     * 수정된 필드 생성.
     */
    public static FieldDiff modified(String fieldName, String fieldLabel, Object before, Object after) {
        return new FieldDiff(fieldName, fieldLabel, before, after, DiffType.MODIFIED);
    }

    /**
     * 추가된 필드 생성.
     */
    public static FieldDiff added(String fieldName, String fieldLabel, Object value) {
        return new FieldDiff(fieldName, fieldLabel, null, value, DiffType.ADDED);
    }

    /**
     * 삭제된 필드 생성.
     */
    public static FieldDiff removed(String fieldName, String fieldLabel, Object value) {
        return new FieldDiff(fieldName, fieldLabel, value, null, DiffType.REMOVED);
    }
}
