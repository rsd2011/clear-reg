-- 2025-01-10-business-template-mapping.sql
-- Purpose: map business feature + organization to default approval/form templates.
-- Rollback: drop business_template_mappings table.

BEGIN;

CREATE TABLE IF NOT EXISTS business_template_mappings (
    id UUID PRIMARY KEY,
    business_feature_code VARCHAR(100) NOT NULL,
    organization_code VARCHAR(64),
    approval_template_id UUID NOT NULL REFERENCES approval_line_templates(id) ON DELETE RESTRICT,
    form_template_id UUID NOT NULL REFERENCES draft_form_templates(id) ON DELETE RESTRICT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uniq_btm_business_org UNIQUE (business_feature_code, organization_code)
);

CREATE INDEX IF NOT EXISTS idx_btm_org ON business_template_mappings(organization_code);

COMMIT;

-- Rollback Instructions:
-- BEGIN;
-- DROP TABLE IF EXISTS business_template_mappings;
-- COMMIT;
