package com.reliablequeue.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dead_letter_jobs", indexes = {
    @Index(name = "idx_dlq_original_job", columnList = "original_job_id"),
    @Index(name = "idx_dlq_replayed", columnList = "replayed, moved_to_dlq_at")
})
public class DeadLetterJob {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "original_job_id", nullable = false)
    private UUID originalJobId;

    @Column(name = "queue_name", nullable = false, length = 64)
    private String queueName;

    @Column(name = "job_type", nullable = false, length = 64)
    private String jobType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "moved_to_dlq_at", nullable = false)
    private Instant movedToDlqAt = Instant.now();

    @Column(name = "replayed", nullable = false)
    private boolean replayed = false;

    @Column(name = "replayed_at")
    private Instant replayedAt;

    @Column(name = "replayed_as_job_id")
    private UUID replayedAsJobId;

    public DeadLetterJob() {
        this.id = UUID.randomUUID();
        this.movedToDlqAt = Instant.now();
    }

    public DeadLetterJob(UUID originalJobId, String queueName, String jobType, String payload, String failureReason, String stackTrace, int retryCount) {
        this.id = UUID.randomUUID();
        this.originalJobId = originalJobId;
        this.queueName = queueName;
        this.jobType = jobType;
        this.payload = payload;
        this.failureReason = failureReason;
        this.stackTrace = stackTrace;
        this.retryCount = retryCount;
        this.movedToDlqAt = Instant.now();
        this.replayed = false;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOriginalJobId() {
        return originalJobId;
    }

    public void setOriginalJobId(UUID originalJobId) {
        this.originalJobId = originalJobId;
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

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public Instant getMovedToDlqAt() {
        return movedToDlqAt;
    }

    public void setMovedToDlqAt(Instant movedToDlqAt) {
        this.movedToDlqAt = movedToDlqAt;
    }

    public boolean isReplayed() {
        return replayed;
    }

    public void setReplayed(boolean replayed) {
        this.replayed = replayed;
    }

    public Instant getReplayedAt() {
        return replayedAt;
    }

    public void setReplayedAt(Instant replayedAt) {
        this.replayedAt = replayedAt;
    }

    public UUID getReplayedAsJobId() {
        return replayedAsJobId;
    }

    public void setReplayedAsJobId(UUID replayedAsJobId) {
        this.replayedAsJobId = replayedAsJobId;
    }
}
