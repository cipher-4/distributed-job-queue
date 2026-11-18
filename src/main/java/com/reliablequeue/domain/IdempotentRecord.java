package com.reliablequeue.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotent_records")
public class IdempotentRecord {

    @Id
    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "operation_type", nullable = false, length = 64)
    private String operationType;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "result_data", columnDefinition = "TEXT")
    private String resultData;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt = Instant.now();

    public IdempotentRecord() {
    }

    public IdempotentRecord(String idempotencyKey, String operationType, UUID jobId, String status, String resultData) {
        this.idempotencyKey = idempotencyKey;
        this.operationType = operationType;
        this.jobId = jobId;
        this.status = status;
        this.resultData = resultData;
        this.executedAt = Instant.now();
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResultData() {
        return resultData;
    }

    public void setResultData(String resultData) {
        this.resultData = resultData;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }
}
