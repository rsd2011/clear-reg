-- 권한/데이터 정책 통합을 위한 스키마 마이그레이션 (PostgreSQL 기준)
-- 실행 전 전체 백업을 수행하고, 트랜잭션 블록 내에서 적용합니다.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

BEGIN;

-- 1. users 테이블에 조직/권한그룹 컬럼 추가
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS organization_code VARCHAR(100) NOT NULL DEFAULT 'ROOT';

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS permission_group_code VARCHAR(100) NOT NULL DEFAULT 'DEFAULT';

-- 기존 데이터 백필
UPDATE users
SET organization_code = COALESCE(organization_code, 'ROOT'),
    permission_group_code = COALESCE(permission_group_code, 'DEFAULT');

-- 새로운 데이터에 기본값 강제 후 DEFAULT 제거 (응용단에서 명시적으로 입력)
ALTER TABLE users ALTER COLUMN organization_code DROP DEFAULT;
ALTER TABLE users ALTER COLUMN permission_group_code DROP DEFAULT;

CREATE INDEX IF NOT EXISTS idx_users_organization_code ON users (organization_code);
CREATE INDEX IF NOT EXISTS idx_users_perm_group_code ON users (permission_group_code);

-- 2. Permission Group 메타 테이블 생성
CREATE TABLE IF NOT EXISTS permission_groups (
    id UUID PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    default_row_scope VARCHAR(16) NOT NULL,
    CONSTRAINT chk_permission_groups_row_scope
        CHECK (default_row_scope IN ('OWN', 'ORG', 'ALL', 'CUSTOM'))
);

CREATE TABLE IF NOT EXISTS permission_group_assignments (
    group_id UUID NOT NULL REFERENCES permission_groups (id) ON DELETE CASCADE,
    feature_code VARCHAR(100) NOT NULL,
    action_code VARCHAR(50) NOT NULL,
    row_scope VARCHAR(16) NOT NULL,
    CONSTRAINT chk_pga_feature CHECK (feature_code IN (
        'RULE_MANAGE','CUSTOMER','AUDIT_LOG','ORGANIZATION','EMPLOYEE','MENU','POLICY','HR_IMPORT'
    )),
    CONSTRAINT chk_pga_action CHECK (action_code IN (
        'CREATE','READ','UPDATE','DELETE','APPROVE','EXPORT','UNMASK'
    )),
    CONSTRAINT chk_pga_row_scope CHECK (row_scope IN ('OWN','ORG','ALL','CUSTOM')),
    CONSTRAINT pk_permission_group_assignments PRIMARY KEY (group_id, feature_code, action_code)
);

CREATE TABLE IF NOT EXISTS permission_group_mask_rules (
    group_id UUID NOT NULL REFERENCES permission_groups (id) ON DELETE CASCADE,
    mask_tag VARCHAR(100) NOT NULL,
    mask_with VARCHAR(100) NOT NULL DEFAULT '***',
    required_action VARCHAR(50) NOT NULL DEFAULT 'UNMASK',
    audit BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_pgmr_action CHECK (required_action IN (
        'CREATE','READ','UPDATE','DELETE','APPROVE','EXPORT','UNMASK'
    )),
    CONSTRAINT pk_permission_group_mask_rules PRIMARY KEY (group_id, mask_tag)
);

-- 3. 조직 정책 테이블
CREATE TABLE IF NOT EXISTS organization_policies (
    id UUID PRIMARY KEY,
    organization_code VARCHAR(100) NOT NULL UNIQUE,
    default_permission_group_code VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS organization_policy_permission_groups (
    policy_id UUID NOT NULL REFERENCES organization_policies (id) ON DELETE CASCADE,
    permission_group_code VARCHAR(100) NOT NULL,
    CONSTRAINT pk_org_policy_perm_groups PRIMARY KEY (policy_id, permission_group_code)
);

CREATE TABLE IF NOT EXISTS organization_policy_approval_flow (
    policy_id UUID NOT NULL REFERENCES organization_policies (id) ON DELETE CASCADE,
    step_index INTEGER NOT NULL,
    approval_group_code VARCHAR(100) NOT NULL,
    CONSTRAINT pk_org_policy_flow PRIMARY KEY (policy_id, step_index)
);

CREATE INDEX IF NOT EXISTS idx_org_policy_flow_policy ON organization_policy_approval_flow (policy_id);

-- 4. 초기 Permission Group/Policy 시드 (필요 시 커스터마이즈)
INSERT INTO permission_groups (id, code, name, description, default_row_scope)
VALUES (gen_random_uuid(), 'DEFAULT', '기본 사용자', '조직/메뉴 조회 등 최소 권한', 'ALL')
ON CONFLICT (code) DO NOTHING;

WITH grp AS (
    SELECT id FROM permission_groups WHERE code = 'DEFAULT'
)
INSERT INTO permission_group_assignments (group_id, feature_code, action_code, row_scope)
SELECT grp.id, feature_code, action_code, row_scope
FROM grp
CROSS JOIN (
    VALUES
        ('ORGANIZATION','READ','ALL'),
        ('HR_IMPORT','READ','ALL'),
        ('POLICY','READ','OWN')
) AS perms(feature_code, action_code, row_scope)
ON CONFLICT DO NOTHING;

WITH grp AS (
    SELECT id FROM permission_groups WHERE code = 'DEFAULT'
)
INSERT INTO permission_group_mask_rules (group_id, mask_tag, mask_with, required_action, audit)
SELECT grp.id, 'ORG_NAME', '***', 'READ', FALSE FROM grp
ON CONFLICT DO NOTHING;

INSERT INTO organization_policies (id, organization_code, default_permission_group_code)
VALUES (gen_random_uuid(), 'ROOT', 'DEFAULT')
ON CONFLICT (organization_code) DO NOTHING;

COMMIT;

-- ROLLBACK 전략: 문제가 발생하면 위 블록을 실행하기 전에 트랜잭션을 중단하거나, 생성된 객체들을 역순으로 DROP/ALTER 하십시오.
