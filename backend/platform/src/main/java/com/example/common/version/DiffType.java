package com.example.common.version;

/**
 * 변경 유형.
 * 버전 비교 시 필드나 컬렉션 요소의 변경 유형을 나타냅니다.
 */
public enum DiffType {
    /** 추가됨 */
    ADDED,

    /** 삭제됨 */
    REMOVED,

    /** 수정됨 */
    MODIFIED,

    /** 변경 없음 */
    UNCHANGED
}
