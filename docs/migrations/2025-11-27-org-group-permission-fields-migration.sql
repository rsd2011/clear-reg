-- =============================================================================
-- OrgGroup 권한 필드 마이그레이션
-- =============================================================================
-- 변경 내용:
-- 1. org_group: priority → sort (nullable), PermGroupCode 3개 필드 추가
-- 2. org_group_member: priority → sort (nullable), PermGroupCode 3개 필드 제거
-- =============================================================================

-- Step 1: org_group 테이블 변경
-- -----------------------------------------------------------------------------

-- priority 컬럼을 sort로 이름 변경 (nullable)
ALTER TABLE org_group RENAME COLUMN priority TO sort;

-- PermGroupCode 필드 3개 추가 (기존 org_group_member에서 이동)
ALTER TABLE org_group ADD COLUMN leader_perm_group_code VARCHAR(100);
ALTER TABLE org_group ADD COLUMN manager_perm_group_code VARCHAR(100);
ALTER TABLE org_group ADD COLUMN member_perm_group_code VARCHAR(100);

-- Step 2: org_group_member 데이터를 org_group으로 마이그레이션
-- -----------------------------------------------------------------------------
-- 참고: org_group_member의 각 멤버가 가진 PermGroupCode가 동일한 그룹에 소속된 경우,
--       그룹별로 첫 번째 멤버의 값을 사용합니다.
--       데이터 마이그레이션 전에 기존 데이터를 검토하세요.

UPDATE org_group g
SET leader_perm_group_code = (
    SELECT m.leader_perm_group_code
    FROM org_group_member m
    WHERE m.group_code = g.code
      AND m.leader_perm_group_code IS NOT NULL
    ORDER BY m.priority ASC NULLS LAST
    LIMIT 1
),
manager_perm_group_code = (
    SELECT m.manager_perm_group_code
    FROM org_group_member m
    WHERE m.group_code = g.code
      AND m.manager_perm_group_code IS NOT NULL
    ORDER BY m.priority ASC NULLS LAST
    LIMIT 1
),
member_perm_group_code = (
    SELECT m.member_perm_group_code
    FROM org_group_member m
    WHERE m.group_code = g.code
      AND m.member_perm_group_code IS NOT NULL
    ORDER BY m.priority ASC NULLS LAST
    LIMIT 1
);

-- Step 3: org_group_member 테이블 변경
-- -----------------------------------------------------------------------------

-- priority 컬럼을 sort로 이름 변경 (nullable)
ALTER TABLE org_group_member RENAME COLUMN priority TO sort;

-- PermGroupCode 필드 3개 제거
ALTER TABLE org_group_member DROP COLUMN IF EXISTS leader_perm_group_code;
ALTER TABLE org_group_member DROP COLUMN IF EXISTS manager_perm_group_code;
ALTER TABLE org_group_member DROP COLUMN IF EXISTS member_perm_group_code;

-- =============================================================================
-- 롤백 스크립트
-- =============================================================================
-- 아래 스크립트는 롤백이 필요할 때 실행하세요.
-- 주의: 데이터 마이그레이션 롤백은 데이터 손실이 발생할 수 있습니다.
-- =============================================================================
/*
-- Step 1: org_group_member에 PermGroupCode 필드 복원
ALTER TABLE org_group_member ADD COLUMN leader_perm_group_code VARCHAR(100);
ALTER TABLE org_group_member ADD COLUMN manager_perm_group_code VARCHAR(100);
ALTER TABLE org_group_member ADD COLUMN member_perm_group_code VARCHAR(100);

-- Step 2: org_group에서 데이터를 org_group_member로 복원 (그룹별 모든 멤버에 동일 값 적용)
UPDATE org_group_member m
SET leader_perm_group_code = g.leader_perm_group_code,
    manager_perm_group_code = g.manager_perm_group_code,
    member_perm_group_code = g.member_perm_group_code
FROM org_group g
WHERE m.group_code = g.code;

-- Step 3: sort 컬럼을 priority로 이름 변경
ALTER TABLE org_group_member RENAME COLUMN sort TO priority;
ALTER TABLE org_group RENAME COLUMN sort TO priority;

-- Step 4: org_group에서 PermGroupCode 필드 제거
ALTER TABLE org_group DROP COLUMN IF EXISTS leader_perm_group_code;
ALTER TABLE org_group DROP COLUMN IF EXISTS manager_perm_group_code;
ALTER TABLE org_group DROP COLUMN IF EXISTS member_perm_group_code;
*/
