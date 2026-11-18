# Reliable Background Job Queue Engine

A high-throughput, crash-resilient background task execution engine built with **Java 21**, **Spring Boot 3.3**, and **PostgreSQL**. Implements atomic worker leases, exponential backoff retries, dead-letter recovery, job dependency chaining, and strict idempotency guarantees.

---

## Key Capabilities

- **Atomic Worker Claims (`FOR UPDATE SKIP LOCKED`)**: Eliminates worker contention and race conditions at the database layer. Multiple worker nodes claim non-overlapping job batches concurrently without explicit table locking.
- **Crash Recovery & Visibility Leases**: When a worker claims a job, it obtains a time-bound lease (`lease_expires_at`). A heartbeat thread periodically renews active leases. If a node crashes mid-execution, a zombie reaper reclaims expired leases and reschedules the jobs automatically.
- **Exponential Backoff & DLQ Isolation**: Failed jobs retry according to `base * 2^(attempt - 1)`. When maximum retry limits are reached, jobs transition to a dedicated Dead-Letter Queue (DLQ) preserving root-cause stack traces and diagnostic payloads for one-click replaying.
- **Strict Idempotency Ledger**: Uses unique idempotency keys mapped to an idempotent execution record table, guaranteeing that retried operations (e.g. payment processing) never duplicate side effects.
- **Job Dependency Graph**: Supports parent-child job workflows where dependent tasks remain locked in `PENDING` state until prerequisite tasks complete successfully.
- **Decoupled Architecture**: Can run in unified mode (`ALL_IN_ONE`), API-only producer mode (`PRODUCER`), or dedicated worker consumer mode (`WORKER`), enabling independent horizontal scaling.
- **Real-Time Web Dashboard**: Built-in visual monitoring dashboard (`/index.html`) providing queue depth metrics, active worker leases, live status filtering, and dead-letter replay controls.

---

## Architecture Overview

```
+-------------------------------------------------------------------------+
|                              PRODUCER LAYER                             |
|   REST APIs / Scheduled Dispatchers / Web Management Dashboard          |
+-------------------------------------------------------------------------+
                                     |
                          1. Enqueue (Durable SQL)
                                     v
+-------------------------------------------------------------------------+
|                          POSTGRESQL QUEUE STORE                         |
|   - jobs (status, priority, scheduled_at, lease_expires_at)             |
|   - dead_letter_jobs (failure reasons, stack traces, replay audit)      |
|   - idempotent_records (operation ledger & execution locks)             |
+-------------------------------------------------------------------------+
             ^                                              ^
 2. Claim    | (FOR UPDATE SKIP LOCKED)         2. Claim    | (FOR UPDATE SKIP LOCKED)
             v                                              v
+--------------------------+                   +--------------------------+
|      WORKER NODE 1       |                   |      WORKER NODE 2       |
|  - Worker Thread Pool    |                   |  - Worker Thread Pool    |
|  - Lease Heartbeat Loop  |                   |  - Lease Heartbeat Loop  |
|  - Zombie Lease Reaper   |                   |  - Zombie Lease Reaper   |
+--------------------------+                   +--------------------------+
```

---

## Core Schema Design

The engine leverages PostgreSQL relational indexing to ensure sub-millisecond polling latency:

```sql
-- Main Queue Table
CREATE TABLE jobs (
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
    depends_on_job_id UUID REFERENCES jobs(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    failed_at TIMESTAMP WITH TIME ZONE,
    last_error TEXT
);

CREATE INDEX idx_jobs_claim_status ON jobs (status, scheduled_at, priority DESC, created_at ASC);
CREATE INDEX idx_jobs_lease ON jobs (status, lease_expires_at);
```

---

## REST API Specification

### 1. Enqueue Job
`POST /api/jobs`

```json
{
  "jobType": "PAYMENT_PROCESS",
  "priority": 8,
  "delaySeconds": 0,
  "maxRetries": 3,
  "backoffBaseSeconds": 2,
  "idempotencyKey": "order_tx_984712",
  "payload": "{\"amount\": 149.99, \"accountId\": \"acc_001\"}"
}
```

### 2. Inspect Queue Metrics
`GET /api/queue/stats`

```json
{
  "totalJobs": 42,
  "pendingJobs": 3,
  "delayedScheduledJobs": 1,
  "runningJobs": 2,
  "completedJobs": 35,
  "failedJobs": 0,
  "deadLetterJobs": 1,
  "unreplayedDlqJobs": 1,
  "nodeMode": "ALL_IN_ONE",
  "workerActive": true,
  "workerConcurrency": 4,
  "workerId": "worker-local"
}
```

### 3. List Dead-Letter Queue Items
`GET /api/dlq?size=10`

### 4. Replay Dead-Letter Job
`POST /api/dlq/{id}/replay`

---

## Running the Project

### Prerequisites
- Java 21 JDK
- Maven 3.9+
- Docker & Docker Compose (optional for cluster mode)

### 1. Run Locally (Embedded H2 / PostgreSQL Mode)
```bash
mvn clean package -DskipTests
java -jar target/reliable-job-queue-1.0.0.jar
```
The application starts on `http://localhost:8080` with the embedded dashboard available at `http://localhost:8080/index.html`.

### 2. Run with Docker Compose (Decoupled Producer + 2 Scaled Worker Nodes)
```bash
docker compose up --build
```
This spins up:
- PostgreSQL 16 container (`rjq-postgres`)
- Producer API & UI container (`rjq-producer-api` on port `8080`)
- Worker Node 1 (`rjq-worker-1`)
- Worker Node 2 (`rjq-worker-2`)

### 3. Execute Integration Tests
```bash
mvn test
```
The test suite validates:
- Concurrent atomic claiming with zero duplicates across parallel threads
- Exponential retry calculations and DLQ transition
- Parent-child dependency sequencing
- Idempotency ledger enforcement

---

## Cloud Deployment Assets

The repository contains production cloud infrastructure manifests in the `/aws` folder:
- `aws/cloudformation-template.yml`: Deploys VPC, Amazon RDS PostgreSQL 16, ECS Fargate cluster, task definitions, and IAM roles.
- `aws/terraform/main.tf`: Complete Terraform HCL configuration for AWS ECS and RDS provisioning.
- `aws/ecs-producer-task.json`: Fargate task configuration for API producer services.
- `aws/ecs-worker-task.json`: Fargate task configuration for scalable consumer worker nodes.

---

## License
MIT
