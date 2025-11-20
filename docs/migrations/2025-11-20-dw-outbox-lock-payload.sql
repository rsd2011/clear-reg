-- Extend dw_ingestion_outbox for locking metadata and payload
ALTER TABLE dw_ingestion_outbox
    ADD COLUMN payload TEXT;

ALTER TABLE dw_ingestion_outbox
    ADD COLUMN locked_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE dw_ingestion_outbox
    ADD COLUMN locked_by VARCHAR(100);

-- Rollback guidelines:
-- ALTER TABLE dw_ingestion_outbox DROP COLUMN locked_by;
-- ALTER TABLE dw_ingestion_outbox DROP COLUMN locked_at;
-- ALTER TABLE dw_ingestion_outbox DROP COLUMN payload;
