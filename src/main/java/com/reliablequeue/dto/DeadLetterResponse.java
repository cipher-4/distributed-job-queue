package com.reliablequeue.dto;

import com.reliablequeue.domain.DeadLetterJob;

import java.time.Instant;
import java.util.UUID;

public class DeadLetterResponse {

    private UUID id;
    private UUID originalJobId;
    private String queueName;
    private String jobType;
    private String payload;
    private String failureReason;
    private String stackTrace;
    private int retryCount;
    private Instant movedToDlqAt;
    private boolean replayed;
    private Instant replayedAt;
    private UUID replayedAsJobId;

    public static DeadLetterResponse fromEntity(DeadLetterJob dlq) {
        DeadLetterResponse res = new DeadLetterResponse();
        res.setId(dlq.getId());
        res.setOriginalJobId(dlq.getOriginalJobId());
        res.setQueueName(dlq.getQueueName());
        res.setJobType(dlq.getJobType());
        res.setPayload(dlq.getPayload());
        res.setFailureReason(dlq.getFailureReason());
        res.setStackTrace(dlq.getStackTrace());
        res.setRetryCount(dlq.getRetryCount());
        res.setMovedToDlqAt(dlq.getMovedToDlqAt());
        res.setReplayed(dlq.isReplayed());
        res.setReplayedAt(dlq.getReplayedAt());
        res.setReplayedAsJobId(dlq.getReplayedAsJobId());
        return res;
    }

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
