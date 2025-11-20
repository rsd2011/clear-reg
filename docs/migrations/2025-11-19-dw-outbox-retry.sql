-- dw_ingestion_outbox retry/last_error tracking
-- FORWARD: add retry_count + last_error metadata.
ALTER TABLE dw_ingestion_outbox
    ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE dw_ingestion_outbox
    ADD COLUMN last_error VARCHAR(500);

-- Optional: backfill existing rows (already defaulted to 0/null).
UPDATE dw_ingestion_outbox SET retry_count = 0 WHERE retry_count IS NULL;

-- ROLLBACK: remove the columns (data loss: retry tracking is dropped).
-- ALTER TABLE dw_ingestion_outbox DROP COLUMN last_error;
-- ALTER TABLE dw_ingestion_outbox DROP COLUMN retry_count;
