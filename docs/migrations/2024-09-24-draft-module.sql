-- 2024-09-24-draft-module.sql
-- Purpose: introduce draft workflow tables (drafts, approval groups/templates, approval steps, history).
-- Rollback: drop all newly created tables.

BEGIN;

CREATE TABLE IF NOT EXISTS approval_groups (
    id UUID PRIMARY KEY,
    group_code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    organization_code VARCHAR(64) NOT NULL,
    condition_expression VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS approval_line_templates (
    id UUID PRIMARY KEY,
    template_code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    business_type VARCHAR(100) NOT NULL,
    organization_code VARCHAR(64) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS approval_template_steps (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES approval_line_templates(id) ON DELETE CASCADE,
    step_order INT NOT NULL,
    approval_group_code VARCHAR(64) NOT NULL,
    description VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS drafts (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    business_feature_code VARCHAR(100) NOT NULL,
    organization_code VARCHAR(64) NOT NULL,
    template_code VARCHAR(100) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS draft_approval_steps (
    id UUID PRIMARY KEY,
    draft_id UUID NOT NULL REFERENCES drafts(id) ON DELETE CASCADE,
    step_order INT NOT NULL,
    approval_group_code VARCHAR(64) NOT NULL,
    description VARCHAR(500),
    state VARCHAR(20) NOT NULL,
    acted_by VARCHAR(100),
    acted_at TIMESTAMPTZ,
    comment VARCHAR(2000)
);

CREATE TABLE IF NOT EXISTS draft_history (
    id UUID PRIMARY KEY,
    draft_id UUID NOT NULL REFERENCES drafts(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    actor VARCHAR(100),
    details VARCHAR(2000),
    occurred_at TIMESTAMPTZ NOT NULL
);

COMMIT;

-- Rollback Instructions:
-- BEGIN;
-- DROP TABLE IF EXISTS draft_history;
-- DROP TABLE IF EXISTS draft_approval_steps;
-- DROP TABLE IF EXISTS drafts;
-- DROP TABLE IF EXISTS approval_template_steps;
-- DROP TABLE IF EXISTS approval_line_templates;
-- DROP TABLE IF EXISTS approval_groups;
-- COMMIT;
