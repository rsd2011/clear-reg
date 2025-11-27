-- =============================================================================
-- ApprovalLineTemplate SCD Type 2 이력관리 마이그레이션
--
-- 목적: 승인선 템플릿의 이력관리를 JSON 스냅샷 기반에서 SCD Type 2로 전환
-- 변경 사항:
--   1. approval_line_template_versions 테이블 생성 (SCD Type 2)
--   2. approval_template_step_versions 테이블 생성
--   3. approval_line_templates에 버전 링크 컬럼 추가
--   4. 기존 데이터를 버전 1로 마이그레이션
-- =============================================================================

-- 1. 버전 테이블 생성
CREATE TABLE IF NOT EXISTS approval_line_template_versions (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES approval_line_templates(id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ,  -- NULL = 현재 유효 버전

    -- 비즈니스 필드 스냅샷
    name VARCHAR(255) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT true,

    -- 버전 상태 (Draft/Published)
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',  -- DRAFT, PUBLISHED, HISTORICAL

    -- 감사 필드
    change_action VARCHAR(20) NOT NULL,  -- CREATE, UPDATE, DELETE, COPY, RESTORE, ROLLBACK, DRAFT, PUBLISH
    change_reason VARCHAR(500),  -- 변경 사유 (선택)
    changed_by VARCHAR(100) NOT NULL,
    changed_by_name VARCHAR(100),
    changed_at TIMESTAMPTZ NOT NULL,
    source_template_id UUID,  -- COPY 시 원본 참조
    rollback_from_version INTEGER,  -- ROLLBACK 시 원본 버전 번호
    version_tag VARCHAR(100),  -- 버전 태그 (선택)

    CONSTRAINT uk_altv_template_version UNIQUE (template_id, version),
    CONSTRAINT chk_altv_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'HISTORICAL')),
    CONSTRAINT chk_altv_change_action CHECK (change_action IN ('CREATE', 'UPDATE', 'DELETE', 'COPY', 'RESTORE', 'ROLLBACK', 'DRAFT', 'PUBLISH'))
);

-- 2. Step 버전 테이블 생성
CREATE TABLE IF NOT EXISTS approval_template_step_versions (
    id UUID PRIMARY KEY,
    template_version_id UUID NOT NULL REFERENCES approval_line_template_versions(id) ON DELETE CASCADE,
    step_order INTEGER NOT NULL,
    approval_group_id UUID NOT NULL REFERENCES approval_groups(id),
    approval_group_code VARCHAR(64) NOT NULL,  -- 비정규화 (시점 조회용)
    approval_group_name VARCHAR(255) NOT NULL,  -- 비정규화 (시점 조회용)

    CONSTRAINT uk_atsv_version_order UNIQUE (template_version_id, step_order)
);

-- 3. 메인 테이블에 버전 링크 컬럼 추가
ALTER TABLE approval_line_templates
ADD COLUMN IF NOT EXISTS current_version_id UUID REFERENCES approval_line_template_versions(id),
ADD COLUMN IF NOT EXISTS previous_version_id UUID REFERENCES approval_line_template_versions(id),
ADD COLUMN IF NOT EXISTS next_version_id UUID REFERENCES approval_line_template_versions(id);

-- 4. 인덱스 생성
-- 현재 게시 버전 빠른 조회 (partial index)
CREATE INDEX IF NOT EXISTS idx_altv_template_current
    ON approval_line_template_versions(template_id)
    WHERE valid_to IS NULL AND status = 'PUBLISHED';

-- 시점 조회용 인덱스
CREATE INDEX IF NOT EXISTS idx_altv_valid_range
    ON approval_line_template_versions(template_id, valid_from, valid_to);

-- 버전 순서 조회용 인덱스
CREATE INDEX IF NOT EXISTS idx_altv_template_version
    ON approval_line_template_versions(template_id, version);

-- 초안 조회용 인덱스
CREATE INDEX IF NOT EXISTS idx_altv_template_draft
    ON approval_line_template_versions(template_id)
    WHERE status = 'DRAFT';

-- Step 버전 조회용 인덱스
CREATE INDEX IF NOT EXISTS idx_atsv_template_version
    ON approval_template_step_versions(template_version_id);

-- 5. 기존 데이터 마이그레이션 (기존 템플릿 → 버전 1로)
INSERT INTO approval_line_template_versions (
    id, template_id, version, valid_from, valid_to,
    name, display_order, description, active,
    status, change_action, change_reason, changed_by, changed_by_name, changed_at
)
SELECT
    gen_random_uuid(),
    t.id,
    1,
    COALESCE(t.created_at, NOW()),
    NULL,  -- 현재 유효
    t.name,
    t.display_order,
    t.description,
    t.active,
    'PUBLISHED',
    'CREATE',
    'SCD Type 2 마이그레이션',
    'MIGRATION',
    'System Migration',
    COALESCE(t.created_at, NOW())
FROM approval_line_templates t
WHERE NOT EXISTS (
    SELECT 1 FROM approval_line_template_versions v
    WHERE v.template_id = t.id
);

-- 6. Step 버전 마이그레이션
INSERT INTO approval_template_step_versions (
    id, template_version_id, step_order, approval_group_id,
    approval_group_code, approval_group_name
)
SELECT
    gen_random_uuid(),
    v.id,
    s.step_order,
    s.approval_group_id,
    g.group_code,
    g.name
FROM approval_template_steps s
JOIN approval_line_template_versions v ON v.template_id = s.approval_template_id AND v.valid_to IS NULL
JOIN approval_groups g ON g.id = s.approval_group_id
WHERE NOT EXISTS (
    SELECT 1 FROM approval_template_step_versions sv
    WHERE sv.template_version_id = v.id AND sv.step_order = s.step_order
);

-- 7. 메인 테이블 버전 링크 업데이트
UPDATE approval_line_templates t
SET current_version_id = (
    SELECT v.id FROM approval_line_template_versions v
    WHERE v.template_id = t.id AND v.valid_to IS NULL AND v.status = 'PUBLISHED'
    LIMIT 1
),
previous_version_id = NULL,
next_version_id = NULL
WHERE current_version_id IS NULL;

-- =============================================================================
-- 롤백 SQL (필요 시 실행)
-- =============================================================================
-- 주의: 롤백 시 버전 데이터가 손실됩니다.
--
-- ALTER TABLE approval_line_templates DROP COLUMN IF EXISTS current_version_id;
-- ALTER TABLE approval_line_templates DROP COLUMN IF EXISTS previous_version_id;
-- ALTER TABLE approval_line_templates DROP COLUMN IF EXISTS next_version_id;
-- DROP TABLE IF EXISTS approval_template_step_versions;
-- DROP TABLE IF EXISTS approval_line_template_versions;
-- =============================================================================
