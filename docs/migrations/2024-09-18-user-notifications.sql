-- 사용자 알림(인앱/이메일) 테이블 생성
CREATE TABLE user_notifications (
    id UUID PRIMARY KEY,
    recipient_username VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    link VARCHAR(500),
    metadata TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    read_at TIMESTAMPTZ,
    created_by VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_user_notifications_recipient ON user_notifications(recipient_username, created_at DESC);

-- Rollback
-- DROP TABLE IF EXISTS user_notifications;
