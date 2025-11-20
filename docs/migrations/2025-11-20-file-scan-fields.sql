-- 파일 스캔/보안 필드 추가

ALTER TABLE stored_files
    ADD COLUMN IF NOT EXISTS sha256 VARCHAR(128),
    ADD COLUMN IF NOT EXISTS scan_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS scanned_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS blocked_reason VARCHAR(500);

UPDATE stored_files SET scan_status = 'CLEAN' WHERE scan_status IS NULL;

-- 롤백: ALTER TABLE stored_files DROP COLUMN sha256, DROP COLUMN scan_status, DROP COLUMN scanned_at, DROP COLUMN blocked_reason;
