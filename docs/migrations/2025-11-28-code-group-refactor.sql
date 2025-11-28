-- 2025-11-28-code-group-refactor.sql
-- Purpose: Refactor system_common_codes into separate code_groups and code_items tables (1:N relationship)
-- Changes:
--   - Create code_groups table for group-level information
--   - Create code_items table for item-level information with FK to code_groups
--   - Migrate existing data from system_common_codes
--   - Drop old system_common_codes table after migration
-- Rollback: Instructions provided at the end

BEGIN;

-- ============================================================
-- Step 1: Create new code_groups table
-- ============================================================
CREATE TABLE IF NOT EXISTS code_groups (
    id UUID PRIMARY KEY,
    source VARCHAR(32) NOT NULL,           -- STATIC_ENUM, DYNAMIC_DB, DW, APPROVAL_GROUP
    group_code VARCHAR(64) NOT NULL,       -- was: code_type
    group_name VARCHAR(255) NOT NULL,
    description VARCHAR(512),
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    metadata JSONB,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(128) NOT NULL,
    
    CONSTRAINT uk_code_group_source_code UNIQUE (source, group_code)
);

COMMENT ON TABLE code_groups IS '코드 그룹 테이블 - 공통코드 그룹 정보 관리';
COMMENT ON COLUMN code_groups.source IS '소스 타입: STATIC_ENUM, DYNAMIC_DB, DW, APPROVAL_GROUP';
COMMENT ON COLUMN code_groups.group_code IS '그룹 코드 (구 code_type)';

-- ============================================================
-- Step 2: Create new code_items table
-- ============================================================
CREATE TABLE IF NOT EXISTS code_items (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL,
    item_code VARCHAR(128) NOT NULL,       -- was: code_value
    item_name VARCHAR(255) NOT NULL,       -- was: code_name
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(512),
    metadata JSONB,                        -- was: metadata
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(128) NOT NULL,
    
    CONSTRAINT fk_code_items_group FOREIGN KEY (group_id) REFERENCES code_groups(id) ON DELETE CASCADE,
    CONSTRAINT uk_code_item_group_code UNIQUE (group_id, item_code)
);

COMMENT ON TABLE code_items IS '코드 아이템 테이블 - 공통코드 아이템 정보 관리';
COMMENT ON COLUMN code_items.group_id IS '소속 그룹 ID (FK to code_groups)';
COMMENT ON COLUMN code_items.item_code IS '아이템 코드 (구 code_value)';
COMMENT ON COLUMN code_items.item_name IS '아이템 이름 (구 code_name)';

-- ============================================================
-- Step 3: Create indexes for performance
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_code_group_code 
    ON code_groups (group_code);

CREATE INDEX IF NOT EXISTS idx_code_group_source 
    ON code_groups (source);

CREATE INDEX IF NOT EXISTS idx_code_item_group 
    ON code_items (group_id, display_order);

CREATE INDEX IF NOT EXISTS idx_code_item_code 
    ON code_items (item_code);

-- ============================================================
-- Step 4: Migrate data from system_common_codes
-- ============================================================

-- 4.1: Insert unique groups from existing data
INSERT INTO code_groups (id, source, group_code, group_name, description, display_order, active, metadata, updated_at, updated_by)
SELECT 
    gen_random_uuid(),
    COALESCE(code_kind, 'DYNAMIC_DB')::VARCHAR(32) AS source,  -- map code_kind to source
    code_type AS group_code,
    code_type AS group_name,  -- use code_type as default name (can be updated later)
    NULL AS description,
    0 AS display_order,
    TRUE AS active,
    NULL AS metadata,
    MAX(updated_at) AS updated_at,
    (ARRAY_AGG(updated_by ORDER BY updated_at DESC))[1] AS updated_by
FROM system_common_codes
GROUP BY code_type, code_kind
ON CONFLICT (source, group_code) DO NOTHING;

-- 4.2: Insert items linked to their groups
INSERT INTO code_items (id, group_id, item_code, item_name, display_order, active, description, metadata, updated_at, updated_by)
SELECT 
    scc.id,  -- preserve original ID if UUID
    cg.id AS group_id,
    scc.code_value AS item_code,
    scc.code_name AS item_name,
    scc.display_order,
    scc.active,
    scc.description,
    scc.metadata AS metadata,
    scc.updated_at,
    scc.updated_by
FROM system_common_codes scc
JOIN code_groups cg ON cg.group_code = scc.code_type 
    AND cg.source = COALESCE(scc.code_kind, 'DYNAMIC_DB')
ON CONFLICT (group_id, item_code) DO NOTHING;

-- ============================================================
-- Step 5: Verify migration (optional check)
-- ============================================================
-- SELECT 
--     (SELECT COUNT(*) FROM system_common_codes) AS old_count,
--     (SELECT COUNT(*) FROM code_items) AS new_item_count,
--     (SELECT COUNT(*) FROM code_groups) AS new_group_count;

-- ============================================================
-- Step 6: Drop old table (uncomment when ready)
-- ============================================================
-- WARNING: Only run this after verifying the migration was successful!
-- DROP TABLE IF EXISTS system_common_codes;

COMMIT;

-- ============================================================
-- Rollback Instructions
-- ============================================================
-- BEGIN;
-- 
-- -- Recreate system_common_codes from new tables if needed
-- CREATE TABLE IF NOT EXISTS system_common_codes (
--     id UUID PRIMARY KEY,
--     code_type VARCHAR(64) NOT NULL,
--     code_value VARCHAR(128) NOT NULL,
--     code_name VARCHAR(255) NOT NULL,
--     display_order INTEGER NOT NULL DEFAULT 0,
--     code_kind VARCHAR(16) NOT NULL,
--     active BOOLEAN NOT NULL DEFAULT TRUE,
--     description VARCHAR(512),
--     metadata JSONB,
--     updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
--     updated_by VARCHAR(128) NOT NULL
-- );
-- 
-- -- Migrate data back
-- INSERT INTO system_common_codes (id, code_type, code_value, code_name, display_order, code_kind, active, description, metadata, updated_at, updated_by)
-- SELECT 
--     ci.id,
--     cg.group_code,
--     ci.item_code,
--     ci.item_name,
--     ci.display_order,
--     cg.source,
--     ci.active,
--     ci.description,
--     ci.metadata,
--     ci.updated_at,
--     ci.updated_by
-- FROM code_items ci
-- JOIN code_groups cg ON ci.group_id = cg.id;
-- 
-- -- Drop new tables
-- DROP TABLE IF EXISTS code_items;
-- DROP TABLE IF EXISTS code_groups;
-- 
-- -- Recreate indexes
-- CREATE UNIQUE INDEX IF NOT EXISTS idx_system_common_code_unique
--     ON system_common_codes (code_type, code_value);
-- CREATE INDEX IF NOT EXISTS idx_system_common_code_type_order
--     ON system_common_codes (code_type, display_order, code_value);
-- 
-- COMMIT;
