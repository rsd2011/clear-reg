-- =============================================================================
-- DataPolicy 도메인 분리: RowAccessPolicy + MaskingPolicy
-- =============================================================================
-- 기존 data_policy 테이블을 row_access_policy와 masking_policy로 분리
-- 관련 리팩터링: rowScope와 마스킹 정책을 독립적으로 관리

-- =============================================================================
-- STEP 1: 새로운 테이블 생성
-- =============================================================================

-- 행 수준 접근 정책 테이블
CREATE TABLE IF NOT EXISTS row_access_policy (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feature_code        VARCHAR(100) NOT NULL,
    action_code         VARCHAR(100),
    perm_group_code     VARCHAR(100),
    org_group_code      VARCHAR(100),
    row_scope           VARCHAR(30) NOT NULL,  -- OWN | ORG | ALL | CUSTOM
    priority            INT NOT NULL DEFAULT 100,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from      TIMESTAMP,
    effective_to        TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_row_access_policy_feature_action
    ON row_access_policy(feature_code, action_code);

CREATE INDEX IF NOT EXISTS idx_row_access_policy_active_priority
    ON row_access_policy(active, priority);

COMMENT ON TABLE row_access_policy IS '행 수준 접근 정책 - 사용자가 조회할 수 있는 데이터 행 범위 정의';
COMMENT ON COLUMN row_access_policy.row_scope IS 'OWN|ORG|ALL|CUSTOM - 행 단위 가시범위';

-- 마스킹 정책 테이블
CREATE TABLE IF NOT EXISTS masking_policy (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feature_code        VARCHAR(100) NOT NULL,
    action_code         VARCHAR(100),
    perm_group_code     VARCHAR(100),
    org_group_code      VARCHAR(100),
    data_kind           VARCHAR(100),          -- 민감 데이터 종류 (SSN, PHONE, EMAIL 등)
    mask_rule           VARCHAR(30) NOT NULL,  -- NONE | PARTIAL | FULL | HASH | TOKENIZE
    mask_params         TEXT,                  -- JSON 형식의 마스킹 파라미터
    audit_enabled       BOOLEAN NOT NULL DEFAULT FALSE,
    priority            INT NOT NULL DEFAULT 100,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from      TIMESTAMP,
    effective_to        TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_masking_policy_feature_action
    ON masking_policy(feature_code, action_code);

CREATE INDEX IF NOT EXISTS idx_masking_policy_data_kind
    ON masking_policy(data_kind);

CREATE INDEX IF NOT EXISTS idx_masking_policy_active_priority
    ON masking_policy(active, priority);

COMMENT ON TABLE masking_policy IS '마스킹 정책 - 민감 데이터 필드에 대한 마스킹 규칙 정의';
COMMENT ON COLUMN masking_policy.data_kind IS '민감 데이터 종류 (SSN, PHONE, EMAIL 등), null이면 모든 종류에 적용';
COMMENT ON COLUMN masking_policy.mask_rule IS 'NONE|PARTIAL|FULL|HASH|TOKENIZE - 마스킹 규칙';

-- =============================================================================
-- STEP 2: 기존 data_policy 데이터를 새 테이블로 마이그레이션
-- =============================================================================

-- row_access_policy로 마이그레이션 (row_scope가 있는 정책)
INSERT INTO row_access_policy (
    id, feature_code, action_code, perm_group_code, org_group_code,
    row_scope, priority, active, effective_from, effective_to, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    feature_code, action_code, perm_group_code, org_group_code,
    CASE WHEN row_scope = 'ORG_AND_DESC' THEN 'ORG' ELSE row_scope END,
    priority, active, effective_from, effective_to, created_at, updated_at
FROM data_policy
WHERE row_scope IS NOT NULL;

-- masking_policy로 마이그레이션 (마스킹 규칙이 NONE이 아닌 정책)
INSERT INTO masking_policy (
    id, feature_code, action_code, perm_group_code, org_group_code,
    data_kind, mask_rule, mask_params, audit_enabled,
    priority, active, effective_from, effective_to, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    feature_code, action_code, perm_group_code, org_group_code,
    NULL,  -- data_kind는 별도 설정 필요
    default_mask_rule, mask_params, COALESCE(audit_enabled, FALSE),
    priority, active, effective_from, effective_to, created_at, updated_at
FROM data_policy
WHERE default_mask_rule IS NOT NULL AND default_mask_rule != 'NONE';

-- =============================================================================
-- STEP 3: 기존 data_policy 테이블 정리 (선택적)
-- =============================================================================

-- 기존 테이블 이름 변경 (백업 보관)
-- ALTER TABLE data_policy RENAME TO data_policy_deprecated;

-- 또는 불필요한 컬럼 삭제 (점진적 마이그레이션)
ALTER TABLE data_policy DROP COLUMN IF EXISTS business_type;
ALTER TABLE data_policy DROP COLUMN IF EXISTS org_policy_id;
ALTER TABLE data_policy DROP COLUMN IF EXISTS row_scope_expr;

-- =============================================================================
-- ROLLBACK (필요 시)
-- =============================================================================
-- DROP TABLE IF EXISTS masking_policy;
-- DROP TABLE IF EXISTS row_access_policy;
-- ALTER TABLE data_policy_deprecated RENAME TO data_policy;
