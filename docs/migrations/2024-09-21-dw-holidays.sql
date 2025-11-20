-- 2024-09-21-dw-holidays.sql
-- Purpose: create dw_holidays table to store canonical holiday calendar ingested via DW pipeline.
-- Rollback: drop the table and dependent indexes.

BEGIN;

CREATE TABLE IF NOT EXISTS dw_holidays (
    id UUID PRIMARY KEY,
    holiday_date DATE NOT NULL,
    country_code VARCHAR(5) NOT NULL,
    local_name VARCHAR(255) NOT NULL,
    english_name VARCHAR(255),
    working_day BOOLEAN NOT NULL DEFAULT FALSE,
    synced_at TIMESTAMP WITH TIME ZONE NOT NULL,
    source_batch_id UUID NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_dw_holiday_date_country
    ON dw_holidays(holiday_date, country_code);

COMMIT;

-- Rollback Instructions:
-- BEGIN;
-- DROP TABLE IF EXISTS dw_holidays;
-- COMMIT;
