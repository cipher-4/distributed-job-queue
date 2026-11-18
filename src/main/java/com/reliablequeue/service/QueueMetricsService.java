package com.reliablequeue.service;

import com.reliablequeue.config.QueueProperties;
import com.reliablequeue.domain.JobStatus;
import com.reliablequeue.dto.QueueStatsResponse;
import com.reliablequeue.repository.DeadLetterJobRepository;
import com.reliablequeue.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class QueueMetricsService {

    private final JobRepository jobRepository;
    private final DeadLetterJobRepository dlqRepository;
    private final QueueProperties queueProperties;
    private final JobConsumerService consumerService;

    public QueueMetricsService(
            JobRepository jobRepository,
            DeadLetterJobRepository dlqRepository,
            QueueProperties queueProperties,
            JobConsumerService consumerService
    ) {
        this.jobRepository = jobRepository;
        this.dlqRepository = dlqRepository;
        this.queueProperties = queueProperties;
        this.consumerService = consumerService;
    }

    @Transactional(readOnly = true)
    public QueueStatsResponse getQueueStats() {
        QueueStatsResponse stats = new QueueStatsResponse();
        Instant now = Instant.now();

        long pending = jobRepository.countByStatus(JobStatus.PENDING);
        long delayed = jobRepository.countByStatusAndScheduledAtAfter(JobStatus.PENDING, now);
        long running = jobRepository.countByStatus(JobStatus.RUNNING);
        long completed = jobRepository.countByStatus(JobStatus.COMPLETED);
        long failed = jobRepository.countByStatus(JobStatus.FAILED);
        long dlq = jobRepository.countByStatus(JobStatus.DEAD_LETTER);
        long unreplayedDlq = dlqRepository.countByReplayed(false);
        long total = jobRepository.count();

        stats.setTotalJobs(total);
        stats.setPendingJobs(pending);
        stats.setDelayedScheduledJobs(delayed);
        stats.setRunningJobs(running);
        stats.setCompletedJobs(completed);
        stats.setFailedJobs(failed);
        stats.setDeadLetterJobs(dlq);
        stats.setUnreplayedDlqJobs(unreplayedDlq);

        stats.setNodeMode(queueProperties.getNodeMode());
        stats.setWorkerActive(consumerService.isRunning());
        stats.setWorkerConcurrency(queueProperties.getWorker().getConcurrency());
        stats.setWorkerId(queueProperties.getWorker().getWorkerId());

        Map<String, Long> breakdown = new HashMap<>();
        breakdown.put("PENDING", pending);
        breakdown.put("RUNNING", running);
        breakdown.put("COMPLETED", completed);
        breakdown.put("DEAD_LETTER", dlq);
        stats.setStatusBreakdown(breakdown);

        return stats;
    }
}
