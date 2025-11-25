-- 2025-11-25 모듈 분리: Draft/Approval 테이블 분리 및 승인 요청 참조 추가

-- drafts 테이블에 approval_request_id 컬럼 추가 (NULL 허용, UUID)
ALTER TABLE drafts ADD COLUMN IF NOT EXISTS approval_request_id UUID;
CREATE INDEX IF NOT EXISTS idx_draft_approval_request ON drafts (approval_request_id);

-- approval_* 테이블 신설 (FK 대신 인덱스로 무결성 관리)
CREATE TABLE IF NOT EXISTS approval_groups (
    id UUID PRIMARY KEY,
    group_code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    organization_code VARCHAR(64) NOT NULL,
    condition_expression VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_approval_group_org ON approval_groups(organization_code);

CREATE TABLE IF NOT EXISTS approval_group_members (
    id UUID PRIMARY KEY,
    approval_group_id UUID NOT NULL,
    member_user_id VARCHAR(100) NOT NULL,
    member_org_code VARCHAR(64) NOT NULL,
    condition_expression VARCHAR(1000),
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_approval_group_members_group ON approval_group_members(approval_group_id);

CREATE TABLE IF NOT EXISTS approval_line_templates (
    id UUID PRIMARY KEY,
    template_code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    business_type VARCHAR(100) NOT NULL,
    scope VARCHAR(20) NOT NULL,
    organization_code VARCHAR(64),
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_template_business_org ON approval_line_templates(business_type, organization_code);

CREATE TABLE IF NOT EXISTS approval_template_steps (
    id UUID PRIMARY KEY,
    approval_template_id UUID NOT NULL,
    step_order INT NOT NULL,
    approval_group_code VARCHAR(64) NOT NULL,
    description VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_template_steps_template ON approval_template_steps(approval_template_id);

-- 롤백 가이드
-- ALTER TABLE drafts DROP COLUMN approval_request_id;
-- DROP TABLE IF EXISTS approval_template_steps, approval_line_templates, approval_group_members, approval_groups;
