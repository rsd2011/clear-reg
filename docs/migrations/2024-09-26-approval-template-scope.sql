-- 2024-09-26-approval-template-scope.sql
-- Purpose: introduce scope enum for approval line templates and allow global templates without organization binding.
-- Rollback: drop column and reapply NOT NULL constraint if necessary.

BEGIN;

ALTER TABLE approval_line_templates
    ADD COLUMN scope VARCHAR(20) NOT NULL DEFAULT 'ORGANIZATION';

ALTER TABLE approval_line_templates
    ALTER COLUMN organization_code DROP NOT NULL;

COMMIT;

-- Rollback Instructions:
-- BEGIN;
-- ALTER TABLE approval_line_templates ALTER COLUMN organization_code SET NOT NULL;
-- ALTER TABLE approval_line_templates DROP COLUMN scope;
-- COMMIT;
