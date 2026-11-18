package com.reliablequeue.service;

import com.reliablequeue.config.QueueProperties;
import com.reliablequeue.domain.Job;
import com.reliablequeue.domain.JobStatus;
import com.reliablequeue.dto.CreateJobRequest;
import com.reliablequeue.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class JobProducerService {

    private static final Logger log = LoggerFactory.getLogger(JobProducerService.class);

    private final JobRepository jobRepository;
    private final QueueProperties queueProperties;

    public JobProducerService(JobRepository jobRepository, QueueProperties queueProperties) {
        this.jobRepository = jobRepository;
        this.queueProperties = queueProperties;
    }

    @Transactional
    public Job enqueue(CreateJobRequest request) {
        // 1. Check idempotency key: if job already exists with this key, return it (prevent duplicate enqueue)
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Optional<Job> existing = jobRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                log.info("Job with idempotencyKey '{}' already exists (Job ID: {}), returning existing job",
                        request.getIdempotencyKey(), existing.get().getId());
                return existing.get();
            }
        }

        // 2. Validate dependencies
        if (request.getDependsOnJobId() != null) {
            Optional<Job> parent = jobRepository.findById(request.getDependsOnJobId());
            if (parent.isEmpty()) {
                throw new IllegalArgumentException("Parent job not found for dependsOnJobId: " + request.getDependsOnJobId());
            }
        }

        // 3. Resolve defaults
        int maxRetries = (request.getMaxRetries() != null)
                ? request.getMaxRetries()
                : queueProperties.getRetry().getDefaultMaxRetries();

        int backoffBaseSeconds = (request.getBackoffBaseSeconds() != null)
                ? request.getBackoffBaseSeconds()
                : queueProperties.getRetry().getDefaultBackoffBaseSeconds();

        Instant scheduledAt = request.getScheduledAt();
        if (scheduledAt == null) {
            scheduledAt = Instant.now();
        }

        Job job = new Job(
                UUID.randomUUID(),
                request.getQueueName(),
                request.getJobType(),
                request.getPayload(),
                request.getPriority(),
                maxRetries,
                backoffBaseSeconds,
                scheduledAt,
                request.getIdempotencyKey(),
                request.getDependsOnJobId()
        );
        job.setLeaseDurationSeconds(queueProperties.getWorker().getLeaseDurationSeconds());

        Job saved = jobRepository.save(job);
        log.info("DURABLY ENQUEUED job ID: {} [Type: {}, Priority: {}, ScheduledAt: {}]",
                saved.getId(), saved.getJobType(), saved.getPriority(), saved.getScheduledAt());
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Job> getJob(UUID id) {
        return jobRepository.findById(id);
    }
}
