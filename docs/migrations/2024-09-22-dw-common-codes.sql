-- 2024-09-22-dw-common-codes.sql
-- Purpose: maintain dw_common_codes table storing reference/common codes sourced from DW feeds.
-- Rollback: drop auxiliary indexes and table.

BEGIN;

CREATE TABLE IF NOT EXISTS dw_common_codes (
    id UUID PRIMARY KEY,
    code_type VARCHAR(64) NOT NULL,
    code_value VARCHAR(128) NOT NULL,
    code_name VARCHAR(255) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    category VARCHAR(128),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(512),
    metadata JSONB,
    synced_at TIMESTAMP WITH TIME ZONE NOT NULL,
    source_batch_id UUID NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_dw_common_code_unique
    ON dw_common_codes (code_type, code_value);

CREATE INDEX IF NOT EXISTS idx_dw_common_code_type_order
    ON dw_common_codes (code_type, display_order, code_value);

COMMIT;

-- Rollback Instructions:
-- BEGIN;
-- DROP TABLE IF EXISTS dw_common_codes;
-- COMMIT;
