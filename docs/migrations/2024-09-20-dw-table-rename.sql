-- 2024-09-20-dw-table-rename.sql
-- Purpose: rename legacy HR_* tables/indexes to DW_* equivalents so DW integration naming stays consistent.
-- Rollback: rename tables/indexes back to HR_* form (see bottom instructions).

BEGIN;

ALTER TABLE IF EXISTS hr_import_batches RENAME TO dw_import_batches;
ALTER TABLE IF EXISTS hr_import_errors RENAME TO dw_import_errors;
ALTER TABLE IF EXISTS hr_employee_staging RENAME TO dw_employee_staging;
ALTER TABLE IF EXISTS hr_organization_staging RENAME TO dw_org_staging;
ALTER TABLE IF EXISTS hr_external_feeds RENAME TO dw_source_feeds;
ALTER TABLE IF EXISTS hr_employees RENAME TO dw_employees;
ALTER TABLE IF EXISTS hr_organizations RENAME TO dw_organizations;
ALTER TABLE IF EXISTS hr_ingestion_policies RENAME TO dw_ingestion_policies;

ALTER INDEX IF EXISTS idx_hr_import_batch_file RENAME TO idx_dw_import_batch_file;
ALTER INDEX IF EXISTS idx_hr_error_batch RENAME TO idx_dw_error_batch;
ALTER INDEX IF EXISTS idx_hr_org_staging_batch_code RENAME TO idx_dw_org_staging_batch_code;
ALTER INDEX IF EXISTS idx_hr_org_code RENAME TO idx_dw_org_code;
ALTER INDEX IF EXISTS idx_hr_org_active RENAME TO idx_dw_org_active;
ALTER INDEX IF EXISTS idx_hr_employee_employee_id RENAME TO idx_dw_employee_employee_id;
ALTER INDEX IF EXISTS idx_hr_employee_active RENAME TO idx_dw_employee_active;
ALTER INDEX IF EXISTS idx_hr_staging_batch_employee RENAME TO idx_dw_staging_batch_employee;

COMMIT;

-- Rollback instructions:
-- BEGIN;
-- ALTER TABLE IF EXISTS dw_import_batches RENAME TO hr_import_batches;
-- ALTER TABLE IF EXISTS dw_import_errors RENAME TO hr_import_errors;
-- ALTER TABLE IF EXISTS dw_employee_staging RENAME TO hr_employee_staging;
-- ALTER TABLE IF EXISTS dw_org_staging RENAME TO hr_organization_staging;
-- ALTER TABLE IF EXISTS dw_source_feeds RENAME TO hr_external_feeds;
-- ALTER TABLE IF EXISTS dw_employees RENAME TO hr_employees;
-- ALTER TABLE IF EXISTS dw_organizations RENAME TO hr_organizations;
-- ALTER TABLE IF EXISTS dw_ingestion_policies RENAME TO hr_ingestion_policies;
-- ALTER INDEX IF EXISTS idx_dw_import_batch_file RENAME TO idx_hr_import_batch_file;
-- ALTER INDEX IF EXISTS idx_dw_error_batch RENAME TO idx_hr_error_batch;
-- ALTER INDEX IF EXISTS idx_dw_org_staging_batch_code RENAME TO idx_hr_org_staging_batch_code;
-- ALTER INDEX IF EXISTS idx_dw_org_code RENAME TO idx_hr_org_code;
-- ALTER INDEX IF EXISTS idx_dw_org_active RENAME TO idx_hr_org_active;
-- ALTER INDEX IF EXISTS idx_dw_employee_employee_id RENAME TO idx_hr_employee_employee_id;
-- ALTER INDEX IF EXISTS idx_dw_employee_active RENAME TO idx_hr_employee_active;
-- ALTER INDEX IF EXISTS idx_dw_staging_batch_employee RENAME TO idx_hr_staging_batch_employee;
-- COMMIT;
