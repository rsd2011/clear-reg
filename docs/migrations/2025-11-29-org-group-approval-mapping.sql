-- =============================================================================
-- 조직그룹별 승인선 템플릿 매핑 테이블
-- =============================================================================
-- 변경 내용:
-- 1. org_group_approval_mapping 테이블 생성
--    - 조직그룹(FK) + 업무유형별 승인선 템플릿 매핑
--    - work_type이 NULL이면 해당 조직그룹의 기본(default) 템플릿
-- =============================================================================

-- Step 1: org_group_approval_mapping 테이블 생성
-- -----------------------------------------------------------------------------

CREATE TABLE org_group_approval_mapping (
    id                       UUID PRIMARY KEY,
    org_group_id             UUID NOT NULL,
    work_type                VARCHAR(50),
    approval_template_root_id UUID NOT NULL,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,

    -- 외래 키 제약조건
    CONSTRAINT fk_org_group_approval_mapping_org_group
        FOREIGN KEY (org_group_id) REFERENCES org_group(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_org_group_approval_mapping_template_root
        FOREIGN KEY (approval_template_root_id) REFERENCES approval_template_roots(id)
        ON DELETE RESTRICT,

    -- 유니크 제약조건: 조직그룹 + 업무유형 조합은 유일해야 함
    -- PostgreSQL에서 NULL은 UNIQUE 제약에서 서로 다른 값으로 취급되므로
    -- COALESCE를 사용한 함수형 유니크 인덱스 필요
    CONSTRAINT uk_org_group_work_type
        UNIQUE (org_group_id, work_type)
);

-- Step 2: 인덱스 생성
-- -----------------------------------------------------------------------------

-- 조직그룹 ID 조회 인덱스
CREATE INDEX idx_org_group_approval_mapping_org_group_id
    ON org_group_approval_mapping(org_group_id);

-- 템플릿 루트 ID 조회 인덱스 (템플릿 삭제 시 참조 확인용)
CREATE INDEX idx_org_group_approval_mapping_template_root_id
    ON org_group_approval_mapping(approval_template_root_id);

-- NULL work_type 조회를 위한 부분 인덱스 (기본 템플릿 빠른 조회)
CREATE INDEX idx_org_group_approval_mapping_default
    ON org_group_approval_mapping(org_group_id)
    WHERE work_type IS NULL;

-- Step 3: 코멘트 추가
-- -----------------------------------------------------------------------------

COMMENT ON TABLE org_group_approval_mapping IS '조직그룹별 승인선 템플릿 매핑';
COMMENT ON COLUMN org_group_approval_mapping.id IS '매핑 ID (UUID)';
COMMENT ON COLUMN org_group_approval_mapping.org_group_id IS '조직그룹 ID (FK)';
COMMENT ON COLUMN org_group_approval_mapping.work_type IS '업무유형 (NULL이면 기본 템플릿)';
COMMENT ON COLUMN org_group_approval_mapping.approval_template_root_id IS '승인선 템플릿 루트 ID (FK)';
COMMENT ON COLUMN org_group_approval_mapping.created_at IS '생성 일시';
COMMENT ON COLUMN org_group_approval_mapping.updated_at IS '수정 일시';


-- =============================================================================
-- 롤백 스크립트
-- =============================================================================
-- DROP TABLE IF EXISTS org_group_approval_mapping CASCADE;
-- =============================================================================
