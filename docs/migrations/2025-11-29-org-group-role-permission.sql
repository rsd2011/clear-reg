-- =============================================================================
-- 조직그룹 역할별 권한그룹 매핑 테이블
-- =============================================================================
-- 변경 내용:
-- 1. org_group_role_permission 테이블 생성
--    - 기존 org_group의 leader/manager/member_perm_group_code 필드를 분리
-- 2. 기존 데이터 마이그레이션
-- 3. org_group 테이블에서 권한 컬럼 제거
-- =============================================================================

-- Step 1: org_group_role_permission 테이블 생성
-- -----------------------------------------------------------------------------

CREATE TABLE org_group_role_permission (
    id              UUID PRIMARY KEY,
    org_group_id    UUID NOT NULL,
    role_type       VARCHAR(20) NOT NULL,
    perm_group_code VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,

    -- 외래 키 제약조건
    CONSTRAINT fk_org_group_role_permission_org_group
        FOREIGN KEY (org_group_id) REFERENCES org_group(id)
        ON DELETE CASCADE,

    -- 유니크 제약조건: 조직그룹 + 역할유형 조합은 유일해야 함
    CONSTRAINT uk_org_group_role_type
        UNIQUE (org_group_id, role_type)
);

-- Step 2: 인덱스 생성
-- -----------------------------------------------------------------------------

-- 조직그룹 ID 조회 인덱스
CREATE INDEX idx_org_group_role_permission_org_group_id
    ON org_group_role_permission(org_group_id);

-- 권한그룹 코드 조회 인덱스 (권한그룹 삭제 시 참조 확인용)
CREATE INDEX idx_org_group_role_permission_perm_group_code
    ON org_group_role_permission(perm_group_code);

-- Step 3: 기존 데이터 마이그레이션
-- -----------------------------------------------------------------------------
-- 기존 org_group의 leader/manager/member_perm_group_code를 새 테이블로 이관

-- LEADER 역할 마이그레이션
INSERT INTO org_group_role_permission (id, org_group_id, role_type, perm_group_code, created_at, updated_at)
SELECT
    gen_random_uuid(),
    g.id,
    'LEADER',
    g.leader_perm_group_code,
    NOW(),
    NOW()
FROM org_group g
WHERE g.leader_perm_group_code IS NOT NULL;

-- MANAGER 역할 마이그레이션
INSERT INTO org_group_role_permission (id, org_group_id, role_type, perm_group_code, created_at, updated_at)
SELECT
    gen_random_uuid(),
    g.id,
    'MANAGER',
    g.manager_perm_group_code,
    NOW(),
    NOW()
FROM org_group g
WHERE g.manager_perm_group_code IS NOT NULL;

-- MEMBER 역할 마이그레이션
INSERT INTO org_group_role_permission (id, org_group_id, role_type, perm_group_code, created_at, updated_at)
SELECT
    gen_random_uuid(),
    g.id,
    'MEMBER',
    g.member_perm_group_code,
    NOW(),
    NOW()
FROM org_group g
WHERE g.member_perm_group_code IS NOT NULL;

-- Step 4: org_group 테이블에서 권한 컬럼 제거
-- -----------------------------------------------------------------------------

ALTER TABLE org_group DROP COLUMN IF EXISTS leader_perm_group_code;
ALTER TABLE org_group DROP COLUMN IF EXISTS manager_perm_group_code;
ALTER TABLE org_group DROP COLUMN IF EXISTS member_perm_group_code;

-- Step 5: 코멘트 추가
-- -----------------------------------------------------------------------------

COMMENT ON TABLE org_group_role_permission IS '조직그룹 역할별 권한그룹 매핑';
COMMENT ON COLUMN org_group_role_permission.id IS '매핑 ID (UUID)';
COMMENT ON COLUMN org_group_role_permission.org_group_id IS '조직그룹 ID (FK)';
COMMENT ON COLUMN org_group_role_permission.role_type IS '역할 유형 (LEADER, MANAGER, MEMBER)';
COMMENT ON COLUMN org_group_role_permission.perm_group_code IS '권한그룹 코드';
COMMENT ON COLUMN org_group_role_permission.created_at IS '생성 일시';
COMMENT ON COLUMN org_group_role_permission.updated_at IS '수정 일시';


-- =============================================================================
-- 롤백 스크립트
-- =============================================================================
-- -- org_group 테이블에 권한 컬럼 복구
-- ALTER TABLE org_group ADD COLUMN leader_perm_group_code VARCHAR(100);
-- ALTER TABLE org_group ADD COLUMN manager_perm_group_code VARCHAR(100);
-- ALTER TABLE org_group ADD COLUMN member_perm_group_code VARCHAR(100);
--
-- -- 데이터 복구
-- UPDATE org_group g
-- SET leader_perm_group_code = (
--     SELECT perm_group_code FROM org_group_role_permission rp
--     WHERE rp.org_group_id = g.id AND rp.role_type = 'LEADER'
-- );
-- UPDATE org_group g
-- SET manager_perm_group_code = (
--     SELECT perm_group_code FROM org_group_role_permission rp
--     WHERE rp.org_group_id = g.id AND rp.role_type = 'MANAGER'
-- );
-- UPDATE org_group g
-- SET member_perm_group_code = (
--     SELECT perm_group_code FROM org_group_role_permission rp
--     WHERE rp.org_group_id = g.id AND rp.role_type = 'MEMBER'
-- );
--
-- -- 테이블 삭제
-- DROP TABLE IF EXISTS org_group_role_permission CASCADE;
-- =============================================================================
