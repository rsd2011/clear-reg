-- ============================================================
-- 메뉴 시스템 마이그레이션
--
-- 목적: Capability 기반 메뉴 접근 제어 및 가시성 설정
--
-- 테이블:
--   - menus: 메뉴 정의 (계층 구조)
--   - menu_capabilities: 메뉴별 필요 Capability (다대다)
--   - menu_view_configs: 메뉴 가시성 설정 (역할/조직별)
-- ============================================================

-- 1. menus 테이블 생성
CREATE TABLE IF NOT EXISTS menus (
    id UUID PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    path VARCHAR(500),
    icon VARCHAR(50),
    sort_order INTEGER,
    description VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    parent_id UUID REFERENCES menus(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_menu_code ON menus(code);
CREATE INDEX IF NOT EXISTS idx_menu_parent ON menus(parent_id);
CREATE INDEX IF NOT EXISTS idx_menu_sort_order ON menus(sort_order);
CREATE INDEX IF NOT EXISTS idx_menu_active ON menus(active) WHERE active = TRUE;

COMMENT ON TABLE menus IS '메뉴 정의 테이블. UI 네비게이션 구조를 표현하며 계층 구조를 지원한다.';
COMMENT ON COLUMN menus.code IS '메뉴 고유 코드 (예: DRAFT_MANAGEMENT, ADMIN_POLICY)';
COMMENT ON COLUMN menus.name IS '메뉴 표시명';
COMMENT ON COLUMN menus.path IS 'UI 라우팅 경로';
COMMENT ON COLUMN menus.icon IS '메뉴 아이콘 식별자';
COMMENT ON COLUMN menus.sort_order IS '정렬 순서 (낮을수록 우선)';
COMMENT ON COLUMN menus.parent_id IS '부모 메뉴 ID (NULL이면 최상위 메뉴)';


-- 2. menu_capabilities 테이블 생성 (메뉴-Capability 연결)
CREATE TABLE IF NOT EXISTS menu_capabilities (
    menu_id UUID NOT NULL REFERENCES menus(id) ON DELETE CASCADE,
    feature_code VARCHAR(50) NOT NULL,
    action_code VARCHAR(50) NOT NULL,
    PRIMARY KEY (menu_id, feature_code, action_code)
);

CREATE INDEX IF NOT EXISTS idx_menu_cap_menu ON menu_capabilities(menu_id);
CREATE INDEX IF NOT EXISTS idx_menu_cap_feature ON menu_capabilities(feature_code);
CREATE INDEX IF NOT EXISTS idx_menu_cap_feature_action ON menu_capabilities(feature_code, action_code);

COMMENT ON TABLE menu_capabilities IS '메뉴별 필요 Capability 매핑. 메뉴 접근에 필요한 권한을 정의한다.';
COMMENT ON COLUMN menu_capabilities.feature_code IS 'FeatureCode enum 값 (예: DRAFT, APPROVAL, ORGANIZATION)';
COMMENT ON COLUMN menu_capabilities.action_code IS 'ActionCode enum 값 (예: READ, DRAFT_CREATE, APPROVAL_REVIEW)';


-- 3. menu_view_configs 테이블 생성 (가시성 설정)
CREATE TABLE IF NOT EXISTS menu_view_configs (
    id UUID PRIMARY KEY,
    menu_id UUID NOT NULL REFERENCES menus(id) ON DELETE CASCADE,
    target_type VARCHAR(30) NOT NULL,
    permission_group_code VARCHAR(100),
    org_policy_id BIGINT,
    visibility_action VARCHAR(20) NOT NULL DEFAULT 'SHOW',
    priority INTEGER DEFAULT 0,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_target_type CHECK (target_type IN ('PERMISSION_GROUP', 'ORG_POLICY', 'GLOBAL')),
    CONSTRAINT chk_visibility_action CHECK (visibility_action IN ('SHOW', 'HIDE', 'HIGHLIGHT'))
);

CREATE INDEX IF NOT EXISTS idx_mvc_menu ON menu_view_configs(menu_id);
CREATE INDEX IF NOT EXISTS idx_mvc_perm_group ON menu_view_configs(permission_group_code);
CREATE INDEX IF NOT EXISTS idx_mvc_org_policy ON menu_view_configs(org_policy_id);
CREATE INDEX IF NOT EXISTS idx_mvc_target_type ON menu_view_configs(target_type);
CREATE INDEX IF NOT EXISTS idx_mvc_active ON menu_view_configs(active) WHERE active = TRUE;

COMMENT ON TABLE menu_view_configs IS '메뉴 가시성 설정. Capability 기반 접근 제어 외에 역할/조직별 추가 가시성을 설정한다.';
COMMENT ON COLUMN menu_view_configs.target_type IS '설정 대상 유형: PERMISSION_GROUP(역할), ORG_POLICY(조직), GLOBAL(전체)';
COMMENT ON COLUMN menu_view_configs.permission_group_code IS 'PermissionGroup 코드 (target_type이 PERMISSION_GROUP일 때)';
COMMENT ON COLUMN menu_view_configs.org_policy_id IS 'OrgPolicy ID (target_type이 ORG_POLICY일 때)';
COMMENT ON COLUMN menu_view_configs.visibility_action IS '가시성 동작: SHOW(표시), HIDE(숨김), HIGHLIGHT(강조)';
COMMENT ON COLUMN menu_view_configs.priority IS '우선순위 (낮을수록 먼저 적용)';


-- ============================================================
-- ROLLBACK
-- ============================================================
-- DROP TABLE IF EXISTS menu_view_configs;
-- DROP TABLE IF EXISTS menu_capabilities;
-- DROP TABLE IF EXISTS menus;
