-- 2025-12-03-users-employee-id.sql
-- UserAccount에 사번(employee_id) 필드 추가
-- 
-- 목적: HR 시스템과의 연동을 위해 username과 별개로 사번을 관리
-- 영향: users 테이블에 employee_id 컬럼 추가

-- PostgreSQL
ALTER TABLE users ADD COLUMN employee_id VARCHAR(64);

-- unique 인덱스 (nullable 컬럼에 대한 partial unique index)
CREATE UNIQUE INDEX uk_users_employee_id ON users(employee_id) WHERE employee_id IS NOT NULL;

-- 롤백:
-- DROP INDEX IF EXISTS uk_users_employee_id;
-- ALTER TABLE users DROP COLUMN employee_id;
