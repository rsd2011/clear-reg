package com.example.common.version;

/**
 * 버전 변경 액션 타입.
 * SCD Type 2 이력관리에서 변경 유형을 나타냅니다.
 * 다양한 도메인에서 재사용 가능합니다.
 */
public enum ChangeAction {
    /** 최초 생성 */
    CREATE,

    /** 수정 */
    UPDATE,

    /** 삭제 (비활성화) */
    DELETE,

    /** 복사 (새 엔티티 생성 시) */
    COPY,

    /** 복원 (활성화) */
    RESTORE,

    /** 이전 버전으로 롤백 */
    ROLLBACK,

    /** 초안 저장 (Draft) */
    DRAFT,

    /** 초안 게시 (Publish) */
    PUBLISH
}
