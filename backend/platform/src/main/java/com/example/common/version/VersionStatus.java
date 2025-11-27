package com.example.common.version;

/**
 * 버전 상태.
 * SCD Type 2 이력관리에서 Draft/Published 기능을 위한 버전 상태를 나타냅니다.
 * 다양한 도메인(승인선 템플릿, 정책, 공지사항 등)에서 재사용 가능합니다.
 */
public enum VersionStatus {
    /** 초안 - 아직 게시되지 않은 상태 */
    DRAFT,

    /** 게시됨 - 활성 버전으로 적용 중 */
    PUBLISHED,

    /** 이력 - 과거 버전 (valid_to가 설정됨) */
    HISTORICAL
}
