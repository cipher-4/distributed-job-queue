package com.reliablequeue.service;

import com.reliablequeue.domain.DeadLetterJob;
import com.reliablequeue.domain.Job;
import com.reliablequeue.domain.JobStatus;
import com.reliablequeue.dto.CreateJobRequest;
import com.reliablequeue.repository.DeadLetterJobRepository;
import com.reliablequeue.repository.JobRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"queue.worker.enabled=false"})
public class RetryAndDeadLetterTest {

    @Autowired
    private JobProducerService producerService;

    @Autowired
    private DeadLetterService deadLetterService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private DeadLetterJobRepository dlqRepository;

    @Test
    @DisplayName("Verify DLQ archiving and zero-loss replay API")
    void testDeadLetterArchivalAndReplay() {
        // 1. Create a failing job that exhausted its retries
        CreateJobRequest req = new CreateJobRequest();
        req.setJobType("SIMULATED_FAILURE");
        req.setMaxRetries(2);
        req.setPayload("{\"alwaysFail\": true}");
        Job job = producerService.enqueue(req);

        // Simulate reaching max retries
        job.setRetryCount(2);
        job.setStatus(JobStatus.DEAD_LETTER);
        job.setLastError("Simulated fatal database connection timeout");
        jobRepository.save(job);

        // 2. Archive to DLQ
        DeadLetterJob dlq = deadLetterService.archiveToDeadLetter(job, new RuntimeException("Simulated fatal database connection timeout"));
        assertThat(dlq).isNotNull();
        assertThat(dlq.getOriginalJobId()).isEqualTo(job.getId());
        assertThat(dlq.isReplayed()).isFalse();

        // 3. Inspect DLQ
        Page<DeadLetterJob> dlqList = deadLetterService.getDeadLetterJobs(false, PageRequest.of(0, 10));
        assertThat(dlqList.getContent()).anyMatch(d -> d.getId().equals(dlq.getId()));

        // 4. Replay from DLQ
        Job replayed = deadLetterService.replayJob(dlq.getId());
        assertThat(replayed).isNotNull();
        assertThat(replayed.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(replayed.getRetryCount()).isEqualTo(0);

        // Verify DLQ item marked as replayed
        DeadLetterJob updatedDlq = dlqRepository.findById(dlq.getId()).orElseThrow();
        assertThat(updatedDlq.isReplayed()).isTrue();
        assertThat(updatedDlq.getReplayedAsJobId()).isEqualTo(replayed.getId());

        System.out.println("DLQ and Replay Test PASSED: Verified DLQ isolation and replay as new job ID: " + replayed.getId());
    }
}
