-- 2024-09-19-spring-batch-schema.sql
-- Purpose: introduce Spring Batch metadata tables so batch executions can be audited and restarted safely.
-- Rollback: drop the Spring Batch tables defined below.

BEGIN;

CREATE TABLE IF NOT EXISTS batch_job_instance (
    job_instance_id   BIGSERIAL PRIMARY KEY,
    version           BIGINT,
    job_name          VARCHAR(100) NOT NULL,
    job_key           VARCHAR(32) NOT NULL,
    CONSTRAINT batch_job_instance_uq UNIQUE (job_name, job_key)
);

CREATE TABLE IF NOT EXISTS batch_job_execution (
    job_execution_id  BIGSERIAL PRIMARY KEY,
    version           BIGINT,
    job_instance_id   BIGINT NOT NULL REFERENCES batch_job_instance(job_instance_id),
    create_time       TIMESTAMP WITH TIME ZONE NOT NULL,
    start_time        TIMESTAMP WITH TIME ZONE,
    end_time          TIMESTAMP WITH TIME ZONE,
    status            VARCHAR(10),
    exit_code         VARCHAR(100),
    exit_message      TEXT,
    last_updated      TIMESTAMP WITH TIME ZONE,
    job_configuration_location VARCHAR(250)
);

CREATE TABLE IF NOT EXISTS batch_job_execution_params (
    job_execution_id BIGINT NOT NULL REFERENCES batch_job_execution(job_execution_id),
    parameter_name   VARCHAR(100) NOT NULL,
    parameter_type   VARCHAR(100) NOT NULL,
    parameter_value  TEXT,
    identifying      BOOLEAN NOT NULL,
    PRIMARY KEY (job_execution_id, parameter_name)
);

CREATE TABLE IF NOT EXISTS batch_step_execution (
    step_execution_id BIGSERIAL PRIMARY KEY,
    version           BIGINT,
    step_name         VARCHAR(100) NOT NULL,
    job_execution_id  BIGINT NOT NULL REFERENCES batch_job_execution(job_execution_id),
    start_time        TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time          TIMESTAMP WITH TIME ZONE,
    status            VARCHAR(10),
    commit_count      BIGINT,
    read_count        BIGINT,
    filter_count      BIGINT,
    write_count       BIGINT,
    read_skip_count   BIGINT,
    write_skip_count  BIGINT,
    process_skip_count BIGINT,
    rollback_count    BIGINT,
    exit_code         VARCHAR(100),
    exit_message      TEXT,
    last_updated      TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS batch_step_execution_context (
    step_execution_id BIGINT PRIMARY KEY REFERENCES batch_step_execution(step_execution_id),
    short_context     VARCHAR(2500),
    serialized_context TEXT
);

CREATE TABLE IF NOT EXISTS batch_job_execution_context (
    job_execution_id BIGINT PRIMARY KEY REFERENCES batch_job_execution(job_execution_id),
    short_context    VARCHAR(2500),
    serialized_context TEXT
);

CREATE TABLE IF NOT EXISTS batch_job_execution_seq (id BIGINT PRIMARY KEY);
CREATE TABLE IF NOT EXISTS batch_job_seq (id BIGINT PRIMARY KEY);
CREATE TABLE IF NOT EXISTS batch_step_execution_seq (id BIGINT PRIMARY KEY);

COMMIT;

-- Rollback instructions:
-- BEGIN;
-- DROP TABLE IF EXISTS batch_step_execution_context;
-- DROP TABLE IF EXISTS batch_job_execution_context;
-- DROP TABLE IF EXISTS batch_step_execution;
-- DROP TABLE IF EXISTS batch_job_execution_params;
-- DROP TABLE IF EXISTS batch_job_execution;
-- DROP TABLE IF EXISTS batch_job_instance;
-- DROP TABLE IF EXISTS batch_job_execution_seq;
-- DROP TABLE IF EXISTS batch_job_seq;
-- DROP TABLE IF EXISTS batch_step_execution_seq;
-- COMMIT;
