-- 2025-01-11-draft-references.sql
-- Purpose: add draft_references table to track reference users per draft.
-- Rollback: drop draft_references table.

BEGIN;

CREATE TABLE IF NOT EXISTS draft_references (
    id UUID PRIMARY KEY,
    draft_id UUID NOT NULL REFERENCES drafts(id) ON DELETE CASCADE,
    referenced_user_id VARCHAR(100) NOT NULL,
    referenced_org_code VARCHAR(64),
    added_by VARCHAR(100) NOT NULL,
    added_at TIMESTAMPTZ NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uniq_draft_reference UNIQUE (draft_id, referenced_user_id)
);

CREATE INDEX IF NOT EXISTS idx_draft_ref_draft ON draft_references(draft_id);
CREATE INDEX IF NOT EXISTS idx_draft_ref_user ON draft_references(referenced_user_id, referenced_org_code);

COMMIT;

-- Rollback Instructions:
-- BEGIN;
-- DROP TABLE IF EXISTS draft_references;
-- COMMIT;
