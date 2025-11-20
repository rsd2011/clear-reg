-- 2025-01-14-draft-withdrawn-at.sql
-- Purpose: add withdrawn_at column for draft withdrawals.
-- Rollback: drop column.

BEGIN;

ALTER TABLE drafts
    ADD COLUMN IF NOT EXISTS withdrawn_at TIMESTAMPTZ;

COMMIT;

-- Rollback Instructions:
-- BEGIN;
-- ALTER TABLE drafts DROP COLUMN IF EXISTS withdrawn_at;
-- COMMIT;
