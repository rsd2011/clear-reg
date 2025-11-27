-- 2025-11-27-approval-group-refactor.sql
-- Purpose: Refactor approval_groups table - remove organization_code and condition_expression, add priority field.
-- Rollback: See rollback instructions at bottom.

BEGIN;

-- 1. Add priority column with default value
ALTER TABLE approval_groups ADD COLUMN IF NOT EXISTS priority INTEGER NOT NULL DEFAULT 0;

-- 2. Drop organization_code index first
DROP INDEX IF EXISTS idx_approval_group_org;

-- 3. Drop organization_code column
ALTER TABLE approval_groups DROP COLUMN IF EXISTS organization_code;

-- 4. Drop condition_expression column
ALTER TABLE approval_groups DROP COLUMN IF EXISTS condition_expression;

COMMIT;

-- Rollback Instructions:
-- BEGIN;
-- ALTER TABLE approval_groups ADD COLUMN organization_code VARCHAR(64);
-- ALTER TABLE approval_groups ADD COLUMN condition_expression VARCHAR(1000);
-- CREATE INDEX idx_approval_group_org ON approval_groups(organization_code);
-- ALTER TABLE approval_groups DROP COLUMN priority;
-- COMMIT;
