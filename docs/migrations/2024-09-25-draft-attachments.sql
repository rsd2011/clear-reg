-- 2024-09-25-draft-attachments.sql
-- Purpose: add draft_attachments table to store file references tied to draft workflows.
-- Rollback: drop draft_attachments table.

BEGIN;

CREATE TABLE IF NOT EXISTS draft_attachments (
    id UUID PRIMARY KEY,
    draft_id UUID NOT NULL REFERENCES drafts(id) ON DELETE CASCADE,
    stored_file_id UUID NOT NULL REFERENCES stored_files(id),
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(150),
    file_size BIGINT NOT NULL CHECK (file_size >= 0),
    attached_by VARCHAR(100) NOT NULL,
    attached_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_draft_attachments_draft_id ON draft_attachments(draft_id);

COMMIT;

-- Rollback Instructions:
-- BEGIN;
-- DROP TABLE IF EXISTS draft_attachments;
-- COMMIT;
