-- ========================================================
-- Reliable Job Queue Schema - V1
-- Supports: Atomic Worker Leases (FOR UPDATE SKIP LOCKED),
-- Dead-Letter Queue (DLQ), Priority Scheduling, Idempotency,
-- and Job Dependency Chaining.
-- ========================================================

CREATE TABLE IF NOT EXISTS jobs (
    id UUID PRIMARY KEY,
    queue_name VARCHAR(64) NOT NULL DEFAULT 'default',
    job_type VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    priority INT NOT NULL DEFAULT 5,
    max_retries INT NOT NULL DEFAULT 3,
    retry_count INT NOT NULL DEFAULT 0,
    backoff_base_seconds INT NOT NULL DEFAULT 2,
    scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    claimed_at TIMESTAMP WITH TIME ZONE,
    claimed_by VARCHAR(128),
    lease_duration_seconds BIGINT NOT NULL DEFAULT 30,
    lease_expires_at TIMESTAMP WITH TIME ZONE,
    last_heartbeat_at TIMESTAMP WITH TIME ZONE,
    idempotency_key VARCHAR(128) UNIQUE,
    depends_on_job_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    failed_at TIMESTAMP WITH TIME ZONE,
    last_error TEXT,
    CONSTRAINT fk_jobs_depends_on FOREIGN KEY (depends_on_job_id) REFERENCES jobs(id) ON DELETE SET NULL
);

-- Indices for high-throughput concurrency and queue polling
CREATE INDEX IF NOT EXISTS idx_jobs_claim_status ON jobs (status, scheduled_at, priority DESC, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_jobs_lease ON jobs (status, lease_expires_at);
CREATE INDEX IF NOT EXISTS idx_jobs_queue ON jobs (queue_name, status);
CREATE INDEX IF NOT EXISTS idx_jobs_idempotency ON jobs (idempotency_key);

-- ========================================================
-- Dead Letter Queue (DLQ) Table
-- Isolates jobs that exceeded max retries for inspection & replay
-- ========================================================
CREATE TABLE IF NOT EXISTS dead_letter_jobs (
    id UUID PRIMARY KEY,
    original_job_id UUID NOT NULL,
    queue_name VARCHAR(64) NOT NULL,
    job_type VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    failure_reason TEXT,
    stack_trace TEXT,
    retry_count INT NOT NULL,
    moved_to_dlq_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    replayed BOOLEAN NOT NULL DEFAULT FALSE,
    replayed_at TIMESTAMP WITH TIME ZONE,
    replayed_as_job_id UUID
);

CREATE INDEX IF NOT EXISTS idx_dlq_original_job ON dead_letter_jobs (original_job_id);
CREATE INDEX IF NOT EXISTS idx_dlq_replayed ON dead_letter_jobs (replayed, moved_to_dlq_at DESC);

-- ========================================================
-- Execution Audit Log Table
-- Tracks full historical lifecycle of worker attempts
-- ========================================================
CREATE TABLE IF NOT EXISTS job_execution_logs (
    id BIGSERIAL PRIMARY KEY,
    job_id UUID NOT NULL,
    attempt_number INT NOT NULL,
    worker_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE,
    duration_ms BIGINT,
    error_message TEXT
);

CREATE INDEX IF NOT EXISTS idx_exec_logs_job ON job_execution_logs (job_id, attempt_number);

-- ========================================================
-- Idempotency Ledger Table
-- Proves that partial crashes and retries never double-apply effects
-- ========================================================
CREATE TABLE IF NOT EXISTS idempotent_records (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    operation_type VARCHAR(64) NOT NULL,
    job_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    result_data TEXT,
    executed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
