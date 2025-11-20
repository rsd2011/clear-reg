-- 공지사항 테이블 및 시퀀스 저장소 생성
CREATE TABLE notice_sequences (
    sequence_year INTEGER PRIMARY KEY,
    next_value INTEGER NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE notices (
    id UUID PRIMARY KEY,
    display_number VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL,
    audience VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    publish_at TIMESTAMPTZ,
    expire_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

COMMENT ON TABLE notices IS '콘솔 공지사항 본문 및 메타데이터';

-- Rollback
-- DROP TABLE IF EXISTS notices;
-- DROP TABLE IF EXISTS notice_sequences;
