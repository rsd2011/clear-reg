-- 2025-01-13-draft-approval-step-version.sql
-- Purpose: add optimistic lock column to draft_approval_steps.
-- Rollback: drop version column.

BEGIN;

ALTER TABLE draft_approval_steps
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

COMMIT;

-- Rollback Instructions:
-- BEGIN;
-- ALTER TABLE draft_approval_steps DROP COLUMN IF EXISTS version;
-- COMMIT;
