package com.example.admin.approval.history;

/**
 * 승인선 템플릿 변경 이력 액션 타입.
 */
public enum HistoryAction {
    /** 최초 생성 */
    CREATE,

    /** 수정 */
    UPDATE,

    /** 삭제 (비활성화) */
    DELETE,

    /** 복사 (새 템플릿 생성 시) */
    COPY,

    /** 복원 (활성화) */
    RESTORE
}
