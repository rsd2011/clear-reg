-- file audit outbox for file actions (upload/download/delete)
-- forward
CREATE TABLE IF NOT EXISTS file_audit_outbox (
    id UUID PRIMARY KEY,
    action VARCHAR(50) NOT NULL,
    file_id UUID NOT NULL,
    actor VARCHAR(100) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    available_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_error TEXT
);

CREATE INDEX IF NOT EXISTS idx_file_audit_outbox_pending ON file_audit_outbox(status, available_at);

-- rollback hint
-- DROP TABLE IF EXISTS file_audit_outbox;
