-- 중앙 파일 저장소 메타데이터
CREATE TABLE stored_files (
    id UUID PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(150),
    file_size BIGINT NOT NULL,
    checksum VARCHAR(128),
    owner_username VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    retention_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    last_accessed_at TIMESTAMPTZ,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE stored_file_versions (
    id UUID PRIMARY KEY,
    file_id UUID NOT NULL REFERENCES stored_files(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    checksum VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(100) NOT NULL
);

CREATE TABLE file_access_logs (
    id UUID PRIMARY KEY,
    file_id UUID NOT NULL REFERENCES stored_files(id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,
    actor VARCHAR(100) NOT NULL,
    detail VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX ux_stored_file_version ON stored_file_versions(file_id, version_number);
CREATE INDEX idx_file_access_file_id ON file_access_logs(file_id);

-- Rollback
-- DROP TABLE IF EXISTS file_access_logs;
-- DROP TABLE IF EXISTS stored_file_versions;
-- DROP TABLE IF EXISTS stored_files;
