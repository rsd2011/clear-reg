-- =============================================================================
-- DW 조직 테이블에 리더/매니저 직원번호 필드 추가
-- =============================================================================
-- 변경 내용:
-- dw_organizations 테이블에 leader_employee_id, manager_employee_id 컬럼 추가
-- JIT Provisioning에서 사용자 역할 결정에 사용됨
-- =============================================================================

-- Step 1: dw_organizations 테이블에 직원번호 필드 추가
-- -----------------------------------------------------------------------------

ALTER TABLE dw_organizations ADD COLUMN leader_employee_id VARCHAR(64);
ALTER TABLE dw_organizations ADD COLUMN manager_employee_id VARCHAR(64);

-- 인덱스 추가 (직원번호로 조직 검색 시 성능 최적화)
CREATE INDEX idx_dw_org_leader ON dw_organizations (leader_employee_id) WHERE leader_employee_id IS NOT NULL;
CREATE INDEX idx_dw_org_manager ON dw_organizations (manager_employee_id) WHERE manager_employee_id IS NOT NULL;

-- 코멘트 추가
COMMENT ON COLUMN dw_organizations.leader_employee_id IS '해당 조직의 리더 직원번호';
COMMENT ON COLUMN dw_organizations.manager_employee_id IS '해당 조직의 업무 매니저 직원번호';

-- =============================================================================
-- 롤백 스크립트
-- =============================================================================
/*
DROP INDEX IF EXISTS idx_dw_org_manager;
DROP INDEX IF EXISTS idx_dw_org_leader;
ALTER TABLE dw_organizations DROP COLUMN IF EXISTS manager_employee_id;
ALTER TABLE dw_organizations DROP COLUMN IF EXISTS leader_employee_id;
*/
