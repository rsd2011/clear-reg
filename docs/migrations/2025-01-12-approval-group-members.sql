-- 2025-01-12-approval-group-members.sql
-- Purpose: add approval_group_members table for member/condition management.
-- Rollback: drop approval_group_members table.

BEGIN;

CREATE TABLE IF NOT EXISTS approval_group_members (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES approval_groups(id) ON DELETE CASCADE,
    member_user_id VARCHAR(100) NOT NULL,
    member_org_code VARCHAR(64),
    condition_expression VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uniq_group_member UNIQUE (group_id, member_user_id)
);

CREATE INDEX IF NOT EXISTS idx_agm_group ON approval_group_members(group_id);
CREATE INDEX IF NOT EXISTS idx_agm_user_org ON approval_group_members(member_user_id, member_org_code);

COMMIT;

-- Rollback Instructions:
-- BEGIN;
-- DROP TABLE IF EXISTS approval_group_members;
-- COMMIT;
