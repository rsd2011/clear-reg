-- =============================================================================
-- ApprovalTemplateStep Skippable 필드 추가
--
-- 목적: 승인 단계 스킵 가능 여부 필드 추가
--       skippable=true인 단계는 결재자를 지정하지 않아도 승인 프로세스 통과
-- 변경 사항:
--   1. approval_template_steps 테이블에 skippable 컬럼 추가
--   2. approval_template_step_versions 테이블에 skippable 컬럼 추가
-- =============================================================================

BEGIN;

-- 1. 레거시 테이블에 skippable 컬럼 추가 (기본값: false)
ALTER TABLE approval_template_steps
ADD COLUMN IF NOT EXISTS skippable BOOLEAN NOT NULL DEFAULT false;

-- 2. 버전 테이블에 skippable 컬럼 추가 (기본값: false)
ALTER TABLE approval_template_step_versions
ADD COLUMN IF NOT EXISTS skippable BOOLEAN NOT NULL DEFAULT false;

COMMIT;

-- =============================================================================
-- 롤백 SQL
-- =============================================================================
-- BEGIN;
-- ALTER TABLE approval_template_steps DROP COLUMN IF EXISTS skippable;
-- ALTER TABLE approval_template_step_versions DROP COLUMN IF EXISTS skippable;
-- COMMIT;
