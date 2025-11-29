-- =============================================================================
-- MaskingPolicy 스키마 변경: data_kind → data_kinds (JSON Set)
-- =============================================================================
-- 단일 dataKind 문자열을 다중 DataKind Set (JSON 배열)으로 변경
-- 예: "SSN" → ["SSN"], null → [] (빈 배열 = 모든 종류에 적용)

-- =============================================================================
-- STEP 1: 새 컬럼 추가
-- =============================================================================
ALTER TABLE masking_policy ADD COLUMN IF NOT EXISTS data_kinds TEXT;

-- =============================================================================
-- STEP 2: 기존 data_kind 값을 data_kinds JSON 배열로 마이그레이션
-- =============================================================================
UPDATE masking_policy
SET data_kinds = CASE
    WHEN data_kind IS NULL OR data_kind = '' THEN '[]'
    ELSE '["' || data_kind || '"]'
END
WHERE data_kinds IS NULL;

-- =============================================================================
-- STEP 3: mask_rule, mask_params 컬럼을 masking_enabled로 단순화
-- =============================================================================
ALTER TABLE masking_policy ADD COLUMN IF NOT EXISTS masking_enabled BOOLEAN;

UPDATE masking_policy
SET masking_enabled = CASE
    WHEN mask_rule IS NULL THEN TRUE
    WHEN mask_rule = 'NONE' THEN FALSE
    ELSE TRUE
END
WHERE masking_enabled IS NULL;

-- 기본값 설정
ALTER TABLE masking_policy ALTER COLUMN masking_enabled SET DEFAULT TRUE;

-- =============================================================================
-- STEP 4: 레거시 컬럼 삭제 (선택적 - 마이그레이션 검증 후 실행)
-- =============================================================================
-- 주의: 아래 명령은 마이그레이션 검증 후에만 실행하세요.
-- ALTER TABLE masking_policy DROP COLUMN IF EXISTS data_kind;
-- ALTER TABLE masking_policy DROP COLUMN IF EXISTS mask_rule;
-- ALTER TABLE masking_policy DROP COLUMN IF EXISTS mask_params;

-- =============================================================================
-- STEP 5: 인덱스 업데이트
-- =============================================================================
-- 기존 단일 data_kind 인덱스 삭제 (레거시 컬럼 삭제 후)
-- DROP INDEX IF EXISTS idx_masking_policy_data_kind;

-- 새로운 인덱스 생성 (JSON 배열 검색용 - GIN 인덱스)
-- PostgreSQL에서 JSONB를 사용하는 경우:
-- CREATE INDEX IF NOT EXISTS idx_masking_policy_data_kinds ON masking_policy USING GIN ((data_kinds::jsonb));

-- 단순 TEXT 컬럼의 경우 btree 인덱스:
CREATE INDEX IF NOT EXISTS idx_masking_policy_data_kinds ON masking_policy(data_kinds);

-- =============================================================================
-- 코멘트 업데이트
-- =============================================================================
COMMENT ON COLUMN masking_policy.data_kinds IS '민감 데이터 종류 목록 (JSON 배열: ["SSN", "PHONE"]), 빈 배열이면 모든 종류에 적용';
COMMENT ON COLUMN masking_policy.masking_enabled IS 'true: 마스킹 적용 (블랙리스트), false: 마스킹 해제 (화이트리스트)';

-- =============================================================================
-- ROLLBACK (필요 시)
-- =============================================================================
-- ALTER TABLE masking_policy DROP COLUMN IF EXISTS data_kinds;
-- ALTER TABLE masking_policy DROP COLUMN IF EXISTS masking_enabled;
