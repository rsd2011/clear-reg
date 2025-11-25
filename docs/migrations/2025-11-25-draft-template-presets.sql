-- 2025-11-25-draft-template-presets.sql
-- Purpose: introduce reusable draft template presets with default approval/form payloads.
-- Rollback: drop draft_template_presets table and template_preset_id column on drafts.

BEGIN;

CREATE TABLE IF NOT EXISTS draft_template_presets (
    id UUID PRIMARY KEY,
    preset_code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    business_feature_code VARCHAR(100) NOT NULL,
    scope VARCHAR(20) NOT NULL,
    organization_code VARCHAR(64),
    title_template TEXT NOT NULL,
    content_template TEXT NOT NULL,
    form_template_id UUID NOT NULL REFERENCES draft_form_templates(id) ON DELETE RESTRICT,
    default_approval_template_id UUID REFERENCES approval_line_templates(id) ON DELETE RESTRICT,
    default_form_payload TEXT NOT NULL,
    variables_json TEXT,
    version INT NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_dtp_scope_org CHECK ((scope <> 'ORGANIZATION') OR (organization_code IS NOT NULL))
);

CREATE INDEX IF NOT EXISTS idx_dtp_business_org ON draft_template_presets(business_feature_code, organization_code);
CREATE INDEX IF NOT EXISTS idx_dtp_active ON draft_template_presets(active);

ALTER TABLE drafts ADD COLUMN IF NOT EXISTS template_preset_id UUID REFERENCES draft_template_presets(id);

COMMIT;

-- Rollback Instructions:
-- BEGIN;
-- ALTER TABLE drafts DROP COLUMN IF EXISTS template_preset_id;
-- DROP TABLE IF EXISTS draft_template_presets;
-- COMMIT;
