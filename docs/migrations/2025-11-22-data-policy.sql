-- 데이터 정책 테이블
CREATE TABLE IF NOT EXISTS data_policy (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feature_code        VARCHAR(100) NOT NULL,
    action_code         VARCHAR(100),
    perm_group_code     VARCHAR(100),
    org_policy_id       BIGINT,
    org_group_code      VARCHAR(100),         -- 조직 그룹 코드 (DW 조직을 그룹으로 매핑)
    business_type       VARCHAR(100),
    row_scope           VARCHAR(30) NOT NULL, -- OWN | ORG | ORG_AND_DESC | ALL | CUSTOM
    row_scope_expr      TEXT,                 -- CUSTOM 일 때 선택적으로 사용
    default_mask_rule   VARCHAR(30) NOT NULL, -- NONE | PARTIAL | FULL | HASH | TOKENIZE
    mask_params         JSONB,
    priority            INT NOT NULL DEFAULT 100,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from      TIMESTAMP,
    effective_to        TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_data_policy_feature_action
    ON data_policy(feature_code, action_code);

CREATE INDEX IF NOT EXISTS idx_data_policy_org_group
    ON data_policy(org_group_code);

-- 조직 그룹
CREATE TABLE IF NOT EXISTS org_group (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(100) UNIQUE NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    priority    INT NOT NULL DEFAULT 100
);

CREATE TABLE IF NOT EXISTS org_group_member (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_code  VARCHAR(100) NOT NULL REFERENCES org_group(code),
    org_id      VARCHAR(100) NOT NULL,
    org_name    VARCHAR(255),
    leader_perm_group_code VARCHAR(100),
    member_perm_group_code VARCHAR(100),
    priority    INT NOT NULL DEFAULT 100,
    UNIQUE (group_code, org_id)
);

-- 조직 그룹 카테고리 (영업/준법/감사/임원/대표/이사회 등 코드성)
CREATE TABLE IF NOT EXISTS org_group_category (
    code        VARCHAR(100) PRIMARY KEY,
    label       VARCHAR(255) NOT NULL,
    description VARCHAR(500)
);

-- 조직 그룹과 카테고리 매핑 (다대다)
CREATE TABLE IF NOT EXISTS org_group_category_map (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_code  VARCHAR(100) NOT NULL REFERENCES org_group(code) ON DELETE CASCADE,
    category_code VARCHAR(100) NOT NULL REFERENCES org_group_category(code) ON DELETE CASCADE,
    UNIQUE (group_code, category_code)
);

CREATE INDEX IF NOT EXISTS idx_data_policy_active_priority
    ON data_policy(active, priority);

COMMENT ON TABLE data_policy IS '기능/행위/권한/조직/업무유형별 데이터 접근·마스킹 정책';
COMMENT ON COLUMN data_policy.default_mask_rule IS 'NONE|PARTIAL|FULL|HASH|TOKENIZE 등 마스킹 기본 규칙';
