-- Audit outbox 테이블 생성
-- 용도: 감사 이벤트를 DB에 적재 후 별도 워커/커넥터가 SIEM/Kafka 등으로 전달
-- 롤백 시: 테이블 삭제(운영상 주의)

CREATE TABLE IF NOT EXISTS audit_outbox (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_outbox_status_next ON audit_outbox(status, next_retry_at);
CREATE INDEX IF NOT EXISTS idx_audit_outbox_aggregate ON audit_outbox(aggregate_type, aggregate_id);

-- 롤백: DROP TABLE audit_outbox;
