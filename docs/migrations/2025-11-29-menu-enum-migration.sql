-- ============================================================
-- 메뉴 시스템 Enum 기반 마이그레이션
--
-- 목적: MenuCode enum 기반 메뉴 관리로 전환
--
-- 변경 사항:
--   1. path 컬럼 제거 (enum에서 관리)
--   2. parent_id 컬럼 제거 (계층 구조는 PermissionMenu에서 관리)
--   3. code 컬럼 값을 MenuCode enum 값으로 변환
-- ============================================================

-- 1. 기존 데이터 백업 (필요시 롤백용)
CREATE TABLE IF NOT EXISTS menus_backup_20251129 AS
SELECT * FROM menus;

-- 2. parent_id 외래키 제약 조건 제거
ALTER TABLE menus DROP CONSTRAINT IF EXISTS menus_parent_id_fkey;

-- 3. 불필요한 인덱스 제거
DROP INDEX IF EXISTS idx_menu_parent;

-- 4. path 컬럼 제거 (enum에서 관리)
ALTER TABLE menus DROP COLUMN IF EXISTS path;

-- 5. parent_id 컬럼 제거 (계층 구조는 PermissionMenu에서 관리)
ALTER TABLE menus DROP COLUMN IF EXISTS parent_id;

-- 6. code 컬럼 데이터 변환 (기존 값 → MenuCode enum 값)
-- 예시: 'dashboard' → 'DASHBOARD', 'draft-management' → 'DRAFT' 등
-- 실제 데이터에 맞게 UPDATE 문 추가 필요

-- 기존 kebab-case 또는 snake_case 형식을 UPPER_SNAKE_CASE로 변환
UPDATE menus SET code = UPPER(REPLACE(REPLACE(code, '-', '_'), ' ', '_'))
WHERE code NOT LIKE '%[A-Z]%';

-- 7. 기본 메뉴 코드 삽입 (MenuCode enum 기준)
-- 이 작업은 애플리케이션 시작 시 syncMenusFromEnum()에서 자동 수행됨
-- 수동으로 미리 삽입하려면 아래 주석 해제

/*
INSERT INTO menus (id, code, name, sort_order, active)
VALUES
    (gen_random_uuid(), 'DASHBOARD', 'DASHBOARD', 1, true),
    (gen_random_uuid(), 'DRAFT', 'DRAFT', 10, true),
    (gen_random_uuid(), 'DRAFT_CREATE', 'DRAFT_CREATE', 11, true),
    (gen_random_uuid(), 'APPROVAL', 'APPROVAL', 20, true),
    (gen_random_uuid(), 'APPROVAL_PENDING', 'APPROVAL_PENDING', 21, true),
    (gen_random_uuid(), 'APPROVAL_COMPLETED', 'APPROVAL_COMPLETED', 22, true),
    (gen_random_uuid(), 'ADMIN', 'ADMIN', 100, true),
    (gen_random_uuid(), 'ADMIN_USER', 'ADMIN_USER', 101, true),
    (gen_random_uuid(), 'ADMIN_ORGANIZATION', 'ADMIN_ORGANIZATION', 102, true),
    (gen_random_uuid(), 'ADMIN_PERMISSION', 'ADMIN_PERMISSION', 103, true),
    (gen_random_uuid(), 'ADMIN_POLICY', 'ADMIN_POLICY', 104, true),
    (gen_random_uuid(), 'ADMIN_AUDIT', 'ADMIN_AUDIT', 105, true),
    (gen_random_uuid(), 'ADMIN_SYSTEM', 'ADMIN_SYSTEM', 106, true)
ON CONFLICT (code) DO NOTHING;
*/

-- 8. 컬럼 코멘트 업데이트
COMMENT ON COLUMN menus.code IS 'MenuCode enum 값 (예: DASHBOARD, DRAFT, APPROVAL). path는 enum에서 관리됨.';


-- ============================================================
-- ROLLBACK
-- ============================================================
-- 롤백 시 백업 테이블에서 복원
-- DROP TABLE IF EXISTS menus;
-- ALTER TABLE menus_backup_20251129 RENAME TO menus;
--
-- 또는 컬럼 복원
-- ALTER TABLE menus ADD COLUMN path VARCHAR(500);
-- ALTER TABLE menus ADD COLUMN parent_id UUID REFERENCES menus(id) ON DELETE SET NULL;
-- CREATE INDEX idx_menu_parent ON menus(parent_id);
