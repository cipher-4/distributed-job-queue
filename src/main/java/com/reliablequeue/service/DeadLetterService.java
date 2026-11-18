package com.reliablequeue.service;

import com.reliablequeue.domain.DeadLetterJob;
import com.reliablequeue.domain.Job;
import com.reliablequeue.domain.JobStatus;
import com.reliablequeue.repository.DeadLetterJobRepository;
import com.reliablequeue.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DeadLetterService {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterService.class);

    private final DeadLetterJobRepository dlqRepository;
    private final JobRepository jobRepository;

    public DeadLetterService(DeadLetterJobRepository dlqRepository, JobRepository jobRepository) {
        this.dlqRepository = dlqRepository;
        this.jobRepository = jobRepository;
    }

    @Transactional
    public DeadLetterJob archiveToDeadLetter(Job job, Throwable error) {
        String failureReason = (error != null) ? error.getMessage() : (job.getLastError() != null ? job.getLastError() : "Max retries exceeded");
        String stackTrace = null;
        if (error != null) {
            StringWriter sw = new StringWriter();
            error.printStackTrace(new PrintWriter(sw));
            stackTrace = sw.toString();
        }

        DeadLetterJob dlq = new DeadLetterJob(
                job.getId(),
                job.getQueueName(),
                job.getJobType(),
                job.getPayload(),
                failureReason,
                stackTrace,
                job.getRetryCount()
        );

        DeadLetterJob saved = dlqRepository.save(dlq);
        log.warn("[DLQ ARCHIVED] Job {} moved to Dead-Letter Queue. Reason: {}", job.getId(), failureReason);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<DeadLetterJob> getDeadLetterJobs(Boolean replayed, Pageable pageable) {
        if (replayed != null) {
            return dlqRepository.findByReplayedOrderByMovedToDlqAtDesc(replayed, pageable);
        }
        return dlqRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<DeadLetterJob> getDeadLetterJob(UUID id) {
        return dlqRepository.findById(id);
    }

    /**
     * Replays a dead letter job by creating a new pending job with fresh retry budget
     */
    @Transactional
    public Job replayJob(UUID dlqId) {
        DeadLetterJob dlq = dlqRepository.findById(dlqId)
                .orElseThrow(() -> new IllegalArgumentException("DLQ job not found with ID: " + dlqId));

        if (dlq.isReplayed()) {
            throw new IllegalStateException("DLQ job " + dlqId + " was already replayed as job " + dlq.getReplayedAsJobId());
        }

        // Replay creates a new active job with clean retry budget
        Job replayedJob = new Job(
                UUID.randomUUID(),
                dlq.getQueueName(),
                dlq.getJobType(),
                dlq.getPayload(),
                5, // default medium priority
                3, // standard retries
                2, // default backoff
                Instant.now(),
                null, // no idempotency key to force clean replay execution
                null
        );

        Job saved = jobRepository.save(replayedJob);

        dlq.setReplayed(true);
        dlq.setReplayedAt(Instant.now());
        dlq.setReplayedAsJobId(saved.getId());
        dlqRepository.save(dlq);

        log.info("[DLQ REPLAYED] DLQ item {} successfully replayed as new Job ID: {}", dlqId, saved.getId());
        return saved;
    }

    /**
     * Replays all pending (unreplayed) dead letter jobs
     */
    @Transactional
    public int replayAll() {
        List<DeadLetterJob> unreplayed = dlqRepository.findAll().stream()
                .filter(d -> !d.isReplayed())
                .toList();

        for (DeadLetterJob dlq : unreplayed) {
            replayJob(dlq.getId());
        }
        return unreplayed.size();
    }
}
