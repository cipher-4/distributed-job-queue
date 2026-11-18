package com.reliablequeue.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs", indexes = {
    @Index(name = "idx_jobs_claim_status", columnList = "status, scheduled_at, priority, created_at"),
    @Index(name = "idx_jobs_lease", columnList = "status, lease_expires_at"),
    @Index(name = "idx_jobs_queue", columnList = "queue_name, status"),
    @Index(name = "idx_jobs_idempotency", columnList = "idempotency_key", unique = true)
})
public class Job {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "queue_name", nullable = false, length = 64)
    private String queueName = "default";

    @Column(name = "job_type", nullable = false, length = 64)
    private String jobType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "priority", nullable = false)
    private int priority = 5;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries = 3;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "backoff_base_seconds", nullable = false)
    private int backoffBaseSeconds = 2;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt = Instant.now();

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "claimed_by", length = 128)
    private String claimedBy;

    @Column(name = "lease_duration_seconds", nullable = false)
    private long leaseDurationSeconds = 30;

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column(name = "idempotency_key", length = 128, unique = true)
    private String idempotencyKey;

    @Column(name = "depends_on_job_id")
    private UUID dependsOnJobId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;


    public Job() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Job(UUID id, String queueName, String jobType, String payload, int priority, int maxRetries, int backoffBaseSeconds, Instant scheduledAt, String idempotencyKey, UUID dependsOnJobId) {
        this.id = (id != null) ? id : UUID.randomUUID();
        this.queueName = (queueName != null && !queueName.isBlank()) ? queueName : "default";
        this.jobType = jobType;
        this.payload = payload;
        this.priority = priority;
        this.maxRetries = maxRetries;
        this.backoffBaseSeconds = backoffBaseSeconds;
        this.scheduledAt = (scheduledAt != null) ? scheduledAt : Instant.now();
        this.idempotencyKey = idempotencyKey;
        this.dependsOnJobId = dependsOnJobId;
        this.status = JobStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getBackoffBaseSeconds() {
        return backoffBaseSeconds;
    }

    public void setBackoffBaseSeconds(int backoffBaseSeconds) {
        this.backoffBaseSeconds = backoffBaseSeconds;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(Instant claimedAt) {
        this.claimedAt = claimedAt;
    }

    public String getClaimedBy() {
        return claimedBy;
    }

    public void setClaimedBy(String claimedBy) {
        this.claimedBy = claimedBy;
    }

    public long getLeaseDurationSeconds() {
        return leaseDurationSeconds;
    }

    public void setLeaseDurationSeconds(long leaseDurationSeconds) {
        this.leaseDurationSeconds = leaseDurationSeconds;
    }

    public Instant getLeaseExpiresAt() {
        return leaseExpiresAt;
    }

    public void setLeaseExpiresAt(Instant leaseExpiresAt) {
        this.leaseExpiresAt = leaseExpiresAt;
    }

    public Instant getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(Instant lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public UUID getDependsOnJobId() {
        return dependsOnJobId;
    }

    public void setDependsOnJobId(UUID dependsOnJobId) {
        this.dependsOnJobId = dependsOnJobId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}
