package com.reliablequeue.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public class CreateJobRequest {

    private String queueName = "default";

    @NotBlank(message = "jobType is required")
    private String jobType;

    @NotBlank(message = "payload is required")
    private String payload;

    @Min(1)
    @Max(10)
    private int priority = 5;

    @Min(0)
    @Max(10)
    private Integer maxRetries;

    @Min(1)
    private Integer backoffBaseSeconds;

    private Instant scheduledAt;

    private Long delaySeconds;

    private String idempotencyKey;

    private UUID dependsOnJobId;

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

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Integer getBackoffBaseSeconds() {
        return backoffBaseSeconds;
    }

    public void setBackoffBaseSeconds(Integer backoffBaseSeconds) {
        this.backoffBaseSeconds = backoffBaseSeconds;
    }

    public Instant getScheduledAt() {
        if (scheduledAt != null) {
            return scheduledAt;
        }
        if (delaySeconds != null && delaySeconds > 0) {
            return Instant.now().plusSeconds(delaySeconds);
        }
        return Instant.now();
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Long getDelaySeconds() {
        return delaySeconds;
    }

    public void setDelaySeconds(Long delaySeconds) {
        this.delaySeconds = delaySeconds;
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
}
