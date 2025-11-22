-- audit_log 테이블 생성 (운영 DB용)
-- 적용 전 환경별 스토리지/암호화/WORM 요건을 확인할 것.

CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    event_time TIMESTAMPTZ NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    system_name VARCHAR(32),
    module_name VARCHAR(32),
    action VARCHAR(64),
    actor_id VARCHAR(64),
    actor_type VARCHAR(16),
    actor_role VARCHAR(64),
    actor_dept VARCHAR(64),
    subject_type VARCHAR(32),
    subject_key VARCHAR(128),
    channel VARCHAR(32),
    client_ip INET,
    user_agent VARCHAR(256),
    device_id VARCHAR(128),
    success_yn BOOLEAN NOT NULL DEFAULT TRUE,
    result_code VARCHAR(32),
    reason_code VARCHAR(32),
    reason_text VARCHAR(512),
    legal_basis_code VARCHAR(32),
    risk_level VARCHAR(8),
    before_summary VARCHAR(1024),
    after_summary VARCHAR(1024),
    extra_json JSONB,
    hash_chain VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_time ON audit_log(event_time DESC);
CREATE INDEX IF NOT EXISTS idx_audit_actor ON audit_log(actor_id, event_time DESC);
CREATE INDEX IF NOT EXISTS idx_audit_subject ON audit_log(subject_type, subject_key);
CREATE INDEX IF NOT EXISTS idx_audit_type ON audit_log(event_type, module_name);

-- 롤백 지침: 규제 요건을 충족하는 별도 백업 후 테이블을 삭제한다.
-- DROP TABLE audit_log;
