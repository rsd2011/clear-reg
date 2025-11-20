-- 2025-01-15-draft-approval-delegate.sql
-- Purpose: add delegation fields to draft_approval_steps.
-- Rollback: drop columns.

BEGIN;

ALTER TABLE draft_approval_steps
    ADD COLUMN IF NOT EXISTS delegated_to VARCHAR(100),
    ADD COLUMN IF NOT EXISTS delegated_at TIMESTAMPTZ;

COMMIT;

-- Rollback Instructions:
-- BEGIN;
-- ALTER TABLE draft_approval_steps
--     DROP COLUMN IF EXISTS delegated_to,
--     DROP COLUMN IF EXISTS delegated_at;
-- COMMIT;
