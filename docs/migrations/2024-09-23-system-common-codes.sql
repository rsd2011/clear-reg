-- 2024-09-23-system-common-codes.sql
-- Purpose: create system_common_codes table for system-managed common code values with static/dynamic flags.
-- Rollback: drop the table and indexes.

BEGIN;

CREATE TABLE IF NOT EXISTS system_common_codes (
    id UUID PRIMARY KEY,
    code_type VARCHAR(64) NOT NULL,
    code_value VARCHAR(128) NOT NULL,
    code_name VARCHAR(255) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    code_kind VARCHAR(16) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(512),
    metadata JSONB,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(128) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_system_common_code_unique
    ON system_common_codes (code_type, code_value);

CREATE INDEX IF NOT EXISTS idx_system_common_code_type_order
    ON system_common_codes (code_type, display_order, code_value);

COMMIT;

-- Rollback Instructions:
-- BEGIN;
-- DROP TABLE IF EXISTS system_common_codes;
-- COMMIT;
