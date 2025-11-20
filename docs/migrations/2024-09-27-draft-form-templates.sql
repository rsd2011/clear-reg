-- 2024-09-27-draft-form-templates.sql
-- Purpose: introduce draft form templates and store template snapshots/payloads on drafts.
-- Rollback: drop new table and columns.

BEGIN;

CREATE TABLE IF NOT EXISTS draft_form_templates (
    id UUID PRIMARY KEY,
    template_code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    business_type VARCHAR(100) NOT NULL,
    scope VARCHAR(20) NOT NULL DEFAULT 'ORGANIZATION',
    organization_code VARCHAR(64),
    schema_json TEXT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_draft_form_template_business_org
    ON draft_form_templates(business_type, organization_code);

ALTER TABLE drafts
    ADD COLUMN form_template_code VARCHAR(100),
    ADD COLUMN form_template_version INT,
    ADD COLUMN form_template_snapshot TEXT,
    ADD COLUMN form_payload TEXT;

COMMIT;

-- Rollback Instructions:
-- BEGIN;
-- ALTER TABLE drafts
--     DROP COLUMN IF EXISTS form_template_code,
--     DROP COLUMN IF EXISTS form_template_version,
--     DROP COLUMN IF EXISTS form_template_snapshot,
--     DROP COLUMN IF EXISTS form_payload;
-- DROP TABLE IF EXISTS draft_form_templates;
-- COMMIT;
