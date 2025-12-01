-- ============================================================================
-- 시스템 설정 SCD Type 2 버전 관리 테이블 생성
-- 날짜: 2025-11-30
-- 설명: PolicyDocument를 대체하는 새로운 시스템 설정 관리 스키마
--       SCD Type 2 패턴을 적용하여 버전 이력 관리, Draft/Publish 워크플로우 지원
-- ============================================================================

-- 1. 시스템 설정 루트 테이블 (버전 컨테이너)
CREATE TABLE IF NOT EXISTS system_config_roots (
    id UUID PRIMARY KEY,
    config_code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- 버전 링크 (SCD Type 2)
    current_version_id UUID,
    previous_version_id UUID,
    next_version_id UUID
);

-- 인덱스
CREATE UNIQUE INDEX IF NOT EXISTS idx_scr_config_code ON system_config_roots(config_code);

COMMENT ON TABLE system_config_roots IS '시스템 설정 루트 - 버전 컨테이너 역할';
COMMENT ON COLUMN system_config_roots.config_code IS '설정 코드 (예: auth.settings, file.settings, audit.settings)';
COMMENT ON COLUMN system_config_roots.current_version_id IS '현재 활성 버전';
COMMENT ON COLUMN system_config_roots.previous_version_id IS '이전 활성 버전 (롤백 참조용)';
COMMENT ON COLUMN system_config_roots.next_version_id IS '다음 예약 버전 (Draft 또는 결재 대기용)';


-- 2. 시스템 설정 리비전 테이블 (버전 엔티티)
CREATE TABLE IF NOT EXISTS system_config_revisions (
    id UUID PRIMARY KEY,
    root_id UUID NOT NULL REFERENCES system_config_roots(id) ON DELETE CASCADE,
    version INT NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    valid_to TIMESTAMPTZ,

    -- 비즈니스 필드
    yaml_content TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    -- 버전 상태 (Draft/Published/Historical)
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',

    -- 감사 필드
    change_action VARCHAR(20) NOT NULL,
    change_reason VARCHAR(500),
    changed_by VARCHAR(100) NOT NULL,
    changed_by_name VARCHAR(100),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    rollback_from_version INT,
    version_tag VARCHAR(100),

    CONSTRAINT chk_screv_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'HISTORICAL')),
    CONSTRAINT chk_screv_change_action CHECK (change_action IN ('CREATE', 'UPDATE', 'DELETE', 'COPY', 'RESTORE', 'ROLLBACK', 'DRAFT', 'PUBLISH'))
);

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_screv_root_version ON system_config_revisions(root_id, version);
CREATE INDEX IF NOT EXISTS idx_screv_valid_range ON system_config_revisions(root_id, valid_from, valid_to);
CREATE INDEX IF NOT EXISTS idx_screv_status ON system_config_revisions(root_id, status);

-- 유니크 제약 (root_id + version 조합은 유일해야 함)
CREATE UNIQUE INDEX IF NOT EXISTS idx_screv_root_version_unique ON system_config_revisions(root_id, version);

COMMENT ON TABLE system_config_revisions IS '시스템 설정 리비전 - SCD Type 2 버전 관리';
COMMENT ON COLUMN system_config_revisions.version IS '버전 번호 (1부터 순차 증가)';
COMMENT ON COLUMN system_config_revisions.valid_from IS '버전 유효 시작 시점';
COMMENT ON COLUMN system_config_revisions.valid_to IS '버전 유효 종료 시점 (NULL이면 현재 유효)';
COMMENT ON COLUMN system_config_revisions.yaml_content IS 'YAML 형식의 설정 내용';
COMMENT ON COLUMN system_config_revisions.status IS '버전 상태: DRAFT(초안), PUBLISHED(게시됨), HISTORICAL(이력)';
COMMENT ON COLUMN system_config_revisions.change_action IS '변경 액션: CREATE, UPDATE, DELETE, ROLLBACK, DRAFT, PUBLISH 등';
COMMENT ON COLUMN system_config_revisions.rollback_from_version IS 'ROLLBACK 시 롤백 대상 버전 번호';


-- 3. 외래 키 추가 (circular reference 방지를 위해 테이블 생성 후 추가)
ALTER TABLE system_config_roots
    ADD CONSTRAINT fk_scr_current_version
    FOREIGN KEY (current_version_id) REFERENCES system_config_revisions(id) ON DELETE SET NULL;

ALTER TABLE system_config_roots
    ADD CONSTRAINT fk_scr_previous_version
    FOREIGN KEY (previous_version_id) REFERENCES system_config_revisions(id) ON DELETE SET NULL;

ALTER TABLE system_config_roots
    ADD CONSTRAINT fk_scr_next_version
    FOREIGN KEY (next_version_id) REFERENCES system_config_revisions(id) ON DELETE SET NULL;


-- ============================================================================
-- ROLLBACK 스크립트
-- ============================================================================
-- ALTER TABLE system_config_roots DROP CONSTRAINT IF EXISTS fk_scr_current_version;
-- ALTER TABLE system_config_roots DROP CONSTRAINT IF EXISTS fk_scr_previous_version;
-- ALTER TABLE system_config_roots DROP CONSTRAINT IF EXISTS fk_scr_next_version;
-- DROP TABLE IF EXISTS system_config_revisions;
-- DROP TABLE IF EXISTS system_config_roots;
