package com.reliablequeue.dto;

import com.reliablequeue.domain.Job;
import com.reliablequeue.domain.JobStatus;

import java.time.Instant;
import java.util.UUID;

public class JobResponse {

    private UUID id;
    private String queueName;
    private String jobType;
    private String payload;
    private JobStatus status;
    private int priority;
    private int maxRetries;
    private int retryCount;
    private int backoffBaseSeconds;
    private Instant scheduledAt;
    private Instant claimedAt;
    private String claimedBy;
    private Instant leaseExpiresAt;
    private String idempotencyKey;
    private UUID dependsOnJobId;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
    private Instant failedAt;
    private String lastError;

    public static JobResponse fromEntity(Job job) {
        JobResponse res = new JobResponse();
        res.setId(job.getId());
        res.setQueueName(job.getQueueName());
        res.setJobType(job.getJobType());
        res.setPayload(job.getPayload());
        res.setStatus(job.getStatus());
        res.setPriority(job.getPriority());
        res.setMaxRetries(job.getMaxRetries());
        res.setRetryCount(job.getRetryCount());
        res.setBackoffBaseSeconds(job.getBackoffBaseSeconds());
        res.setScheduledAt(job.getScheduledAt());
        res.setClaimedAt(job.getClaimedAt());
        res.setClaimedBy(job.getClaimedBy());
        res.setLeaseExpiresAt(job.getLeaseExpiresAt());
        res.setIdempotencyKey(job.getIdempotencyKey());
        res.setDependsOnJobId(job.getDependsOnJobId());
        res.setCreatedAt(job.getCreatedAt());
        res.setUpdatedAt(job.getUpdatedAt());
        res.setCompletedAt(job.getCompletedAt());
        res.setFailedAt(job.getFailedAt());
        res.setLastError(job.getLastError());
        return res;
    }

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

    public Instant getLeaseExpiresAt() {
        return leaseExpiresAt;
    }

    public void setLeaseExpiresAt(Instant leaseExpiresAt) {
        this.leaseExpiresAt = leaseExpiresAt;
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
