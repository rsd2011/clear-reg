-- =============================================================================
-- 조직그룹 업무카테고리 리팩토링 (OrgGroupCategory → WorkCategory JSON)
-- =============================================================================
-- 변경 내용:
-- 1. org_group 테이블에 work_categories JSON 컬럼 추가
-- 2. 기존 org_group_category_map 데이터를 JSON 배열로 마이그레이션
-- 3. org_group_category_map 테이블 삭제
-- 4. org_group_category 테이블 삭제
-- =============================================================================

-- Step 1: org_group 테이블에 work_categories 컬럼 추가
-- -----------------------------------------------------------------------------
-- JSON 배열로 WorkCategory enum 값 저장 (예: ["COMPLIANCE","SALES"])

ALTER TABLE org_group
    ADD COLUMN work_categories TEXT DEFAULT '[]';

-- Step 2: 데이터 마이그레이션 (기존 매핑 데이터를 JSON으로 변환)
-- -----------------------------------------------------------------------------
-- 기존 org_group_category_map의 category_code를 JSON 배열로 변환

UPDATE org_group g
SET work_categories = (
    SELECT COALESCE(
        json_agg(UPPER(m.category_code)),
        '[]'::json
    )::text
    FROM org_group_category_map m
    WHERE m.group_code = g.code
    AND UPPER(m.category_code) IN ('COMPLIANCE', 'SALES', 'TRADING', 'RISK_MANAGEMENT', 'OPERATIONS')
);

-- NULL 값을 빈 배열로 설정
UPDATE org_group
SET work_categories = '[]'
WHERE work_categories IS NULL;

-- Step 3: 기존 테이블 삭제
-- -----------------------------------------------------------------------------

-- org_group_category_map 테이블 삭제
DROP TABLE IF EXISTS org_group_category_map;

-- org_group_category 테이블 삭제
DROP TABLE IF EXISTS org_group_category;

-- Step 4: 코멘트 추가
-- -----------------------------------------------------------------------------

COMMENT ON COLUMN org_group.work_categories IS '업무카테고리 목록 (JSON 배열, WorkCategory enum)';


-- =============================================================================
-- 롤백 스크립트
-- =============================================================================
-- 주의: 데이터 복구가 필요하면 백업 필요
--
-- -- org_group_category 테이블 복구
-- CREATE TABLE org_group_category (
--     code        VARCHAR(100) PRIMARY KEY,
--     label       VARCHAR(255) NOT NULL,
--     description VARCHAR(500)
-- );
--
-- -- org_group_category_map 테이블 복구
-- CREATE TABLE org_group_category_map (
--     id            UUID PRIMARY KEY,
--     group_code    VARCHAR(100) NOT NULL,
--     category_code VARCHAR(100) NOT NULL,
--     UNIQUE (group_code, category_code)
-- );
--
-- -- work_categories 컬럼 삭제
-- ALTER TABLE org_group DROP COLUMN work_categories;
-- =============================================================================
