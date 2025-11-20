-- Adds conditional attribute support for row scope assignments
ALTER TABLE permission_group_assignments
    ADD COLUMN row_condition_expression VARCHAR(1000);

-- Rollback
-- ALTER TABLE permission_group_assignments DROP COLUMN row_condition_expression;
