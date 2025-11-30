-- OrgGroupMember 테이블 리팩토링 마이그레이션
-- 1. org_name 컬럼 삭제
-- 2. sort 컬럼명을 display_order로 변경
-- 3. group_code 컬럼에 외래키 제약조건 추가

-- ============================================================
-- 1. org_name 컬럼 삭제
-- ============================================================
ALTER TABLE org_group_member DROP COLUMN IF EXISTS org_name;

-- ============================================================
-- 2. sort 컬럼명을 display_order로 변경
-- ============================================================
ALTER TABLE org_group_member RENAME COLUMN sort TO display_order;

-- ============================================================
-- 3. group_code 컬럼에 외래키 제약조건 추가
-- ============================================================
-- 먼저 고아 데이터 확인 (group_code가 org_group.code에 없는 데이터)
-- SELECT * FROM org_group_member m
-- WHERE NOT EXISTS (SELECT 1 FROM org_group g WHERE g.code = m.group_code);

-- 외래키 제약조건 추가
ALTER TABLE org_group_member
    ADD CONSTRAINT fk_org_group_member_org_group
    FOREIGN KEY (group_code) REFERENCES org_group(code);

-- ============================================================
-- Rollback
-- ============================================================
-- ALTER TABLE org_group_member DROP CONSTRAINT IF EXISTS fk_org_group_member_org_group;
-- ALTER TABLE org_group_member RENAME COLUMN display_order TO sort;
-- ALTER TABLE org_group_member ADD COLUMN org_name VARCHAR(255);
