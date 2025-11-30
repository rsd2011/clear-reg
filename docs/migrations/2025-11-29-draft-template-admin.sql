-- =============================================================================
-- 기안 템플릿 (admin 모듈) 테이블 생성
-- =============================================================================
-- 변경 내용:
-- 1. draft_form_templates 테이블 생성 (기안 양식 템플릿)
-- 2. draft_template_presets 테이블 생성 (기안 템플릿 프리셋)
-- 3. org_group_approval_mapping에 draft_template_preset_id 컬럼 추가
-- =============================================================================

-- Step 1: draft_form_templates 테이블 생성
-- -----------------------------------------------------------------------------

CREATE TABLE draft_form_templates (
    id                UUID PRIMARY KEY,
    template_code     VARCHAR(100) NOT NULL UNIQUE,
    name              VARCHAR(255) NOT NULL,
    business_type     VARCHAR(100) NOT NULL,
    scope             VARCHAR(20) NOT NULL DEFAULT 'ORGANIZATION',
    organization_code VARCHAR(64),
    schema_json       TEXT NOT NULL,
    version           INTEGER NOT NULL DEFAULT 1,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,

    -- scope 값 제한
    CONSTRAINT chk_draft_form_template_scope
        CHECK (scope IN ('GLOBAL', 'ORGANIZATION'))
);

-- Step 2: draft_form_templates 인덱스 생성
-- -----------------------------------------------------------------------------

CREATE INDEX idx_form_template_business_org
    ON draft_form_templates(business_type, organization_code);

CREATE INDEX idx_form_template_active
    ON draft_form_templates(active);

CREATE INDEX idx_form_template_scope
    ON draft_form_templates(scope);

-- Step 3: draft_template_presets 테이블 생성
-- -----------------------------------------------------------------------------

CREATE TABLE draft_template_presets (
    id                           UUID PRIMARY KEY,
    preset_code                  VARCHAR(100) NOT NULL UNIQUE,
    name                         VARCHAR(255) NOT NULL,
    business_feature_code        VARCHAR(100) NOT NULL,
    scope                        VARCHAR(20) NOT NULL DEFAULT 'ORGANIZATION',
    organization_code            VARCHAR(64),
    title_template               TEXT NOT NULL,
    content_template             TEXT NOT NULL,
    form_template_id             UUID NOT NULL,
    default_approval_template_id UUID,
    default_form_payload         TEXT NOT NULL DEFAULT '{}',
    variables_json               TEXT,
    version                      INTEGER NOT NULL DEFAULT 1,
    active                       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                   TIMESTAMP WITH TIME ZONE NOT NULL,

    -- 외래 키 제약조건
    CONSTRAINT fk_draft_template_preset_form_template
        FOREIGN KEY (form_template_id) REFERENCES draft_form_templates(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_draft_template_preset_approval_template
        FOREIGN KEY (default_approval_template_id) REFERENCES approval_template_roots(id)
        ON DELETE SET NULL,

    -- scope 값 제한
    CONSTRAINT chk_draft_template_preset_scope
        CHECK (scope IN ('GLOBAL', 'ORGANIZATION'))
);

-- Step 4: draft_template_presets 인덱스 생성
-- -----------------------------------------------------------------------------

CREATE INDEX idx_dtp_business_org
    ON draft_template_presets(business_feature_code, organization_code);

CREATE INDEX idx_dtp_active
    ON draft_template_presets(active);

CREATE INDEX idx_dtp_form_template_id
    ON draft_template_presets(form_template_id);

-- Step 5: org_group_approval_mapping에 draft_template_preset_id 컬럼 추가
-- -----------------------------------------------------------------------------

ALTER TABLE org_group_approval_mapping
    ADD COLUMN draft_template_preset_id UUID;

ALTER TABLE org_group_approval_mapping
    ADD CONSTRAINT fk_org_group_approval_mapping_draft_preset
        FOREIGN KEY (draft_template_preset_id) REFERENCES draft_template_presets(id)
        ON DELETE SET NULL;

CREATE INDEX idx_org_group_approval_mapping_preset_id
    ON org_group_approval_mapping(draft_template_preset_id);

-- Step 6: 코멘트 추가
-- -----------------------------------------------------------------------------

COMMENT ON TABLE draft_form_templates IS '기안 양식 템플릿';
COMMENT ON COLUMN draft_form_templates.id IS '템플릿 ID (UUID)';
COMMENT ON COLUMN draft_form_templates.template_code IS '템플릿 코드 (유일)';
COMMENT ON COLUMN draft_form_templates.name IS '템플릿 이름';
COMMENT ON COLUMN draft_form_templates.business_type IS '비즈니스 유형';
COMMENT ON COLUMN draft_form_templates.scope IS '범위 (GLOBAL, ORGANIZATION)';
COMMENT ON COLUMN draft_form_templates.organization_code IS '조직 코드 (ORGANIZATION 범위일 때)';
COMMENT ON COLUMN draft_form_templates.schema_json IS '양식 스키마 JSON';
COMMENT ON COLUMN draft_form_templates.version IS '버전';
COMMENT ON COLUMN draft_form_templates.active IS '활성화 여부';
COMMENT ON COLUMN draft_form_templates.created_at IS '생성 일시';
COMMENT ON COLUMN draft_form_templates.updated_at IS '수정 일시';

COMMENT ON TABLE draft_template_presets IS '기안 템플릿 프리셋';
COMMENT ON COLUMN draft_template_presets.id IS '프리셋 ID (UUID)';
COMMENT ON COLUMN draft_template_presets.preset_code IS '프리셋 코드 (유일)';
COMMENT ON COLUMN draft_template_presets.name IS '프리셋 이름';
COMMENT ON COLUMN draft_template_presets.business_feature_code IS '비즈니스 기능 코드';
COMMENT ON COLUMN draft_template_presets.scope IS '범위 (GLOBAL, ORGANIZATION)';
COMMENT ON COLUMN draft_template_presets.organization_code IS '조직 코드';
COMMENT ON COLUMN draft_template_presets.title_template IS '제목 템플릿';
COMMENT ON COLUMN draft_template_presets.content_template IS '내용 템플릿';
COMMENT ON COLUMN draft_template_presets.form_template_id IS '양식 템플릿 ID (FK)';
COMMENT ON COLUMN draft_template_presets.default_approval_template_id IS '기본 승인선 템플릿 ID (FK)';
COMMENT ON COLUMN draft_template_presets.default_form_payload IS '기본 양식 페이로드 JSON';
COMMENT ON COLUMN draft_template_presets.variables_json IS '변수 JSON';
COMMENT ON COLUMN draft_template_presets.version IS '버전';
COMMENT ON COLUMN draft_template_presets.active IS '활성화 여부';
COMMENT ON COLUMN draft_template_presets.created_at IS '생성 일시';
COMMENT ON COLUMN draft_template_presets.updated_at IS '수정 일시';

COMMENT ON COLUMN org_group_approval_mapping.draft_template_preset_id IS '기안 템플릿 프리셋 ID (FK)';


-- =============================================================================
-- 롤백 스크립트
-- =============================================================================
-- -- org_group_approval_mapping에서 컬럼 제거
-- ALTER TABLE org_group_approval_mapping DROP COLUMN IF EXISTS draft_template_preset_id;
--
-- -- 테이블 삭제
-- DROP TABLE IF EXISTS draft_template_presets CASCADE;
-- DROP TABLE IF EXISTS draft_form_templates CASCADE;
-- =============================================================================
