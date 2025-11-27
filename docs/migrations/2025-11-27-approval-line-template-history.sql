-- 승인선 템플릿 변경 이력 테이블
-- 템플릿의 생성, 수정, 삭제, 복사, 복원 이력을 JSON 스냅샷으로 저장

CREATE TABLE IF NOT EXISTS approval_line_template_history (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL,
    changed_by VARCHAR(100) NOT NULL,
    changed_by_name VARCHAR(100),
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    previous_snapshot JSONB,
    current_snapshot JSONB,
    source_template_id UUID,

    CONSTRAINT fk_history_template
        FOREIGN KEY (template_id)
        REFERENCES approval_line_templates(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_history_source_template
        FOREIGN KEY (source_template_id)
        REFERENCES approval_line_templates(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_action_type
        CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'COPY', 'RESTORE'))
);

CREATE INDEX IF NOT EXISTS idx_alt_history_template_id ON approval_line_template_history(template_id);
CREATE INDEX IF NOT EXISTS idx_alt_history_changed_at ON approval_line_template_history(changed_at DESC);
CREATE INDEX IF NOT EXISTS idx_alt_history_action ON approval_line_template_history(action);

COMMENT ON TABLE approval_line_template_history IS '승인선 템플릿 변경 이력';
COMMENT ON COLUMN approval_line_template_history.template_id IS '대상 템플릿 ID';
COMMENT ON COLUMN approval_line_template_history.action IS '변경 액션: CREATE, UPDATE, DELETE, COPY, RESTORE';
COMMENT ON COLUMN approval_line_template_history.changed_by IS '변경자 ID';
COMMENT ON COLUMN approval_line_template_history.changed_by_name IS '변경자 이름';
COMMENT ON COLUMN approval_line_template_history.changed_at IS '변경 일시';
COMMENT ON COLUMN approval_line_template_history.previous_snapshot IS '변경 전 상태 (JSON)';
COMMENT ON COLUMN approval_line_template_history.current_snapshot IS '변경 후 상태 (JSON)';
COMMENT ON COLUMN approval_line_template_history.source_template_id IS '복사 시 원본 템플릿 ID (COPY 액션에서만 사용)';

-- Rollback
-- DROP TABLE IF EXISTS approval_line_template_history;
