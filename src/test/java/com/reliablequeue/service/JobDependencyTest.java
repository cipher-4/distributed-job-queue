package com.reliablequeue.service;

import com.reliablequeue.domain.Job;
import com.reliablequeue.domain.JobStatus;
import com.reliablequeue.dto.CreateJobRequest;
import com.reliablequeue.repository.JobRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"queue.worker.enabled=false"})
public class JobDependencyTest {

    @Autowired
    private JobProducerService producerService;

    @Autowired
    private JobRepository jobRepository;

    @Test
    @DisplayName("Verify dependent Job B cannot be claimed until parent Job A is COMPLETED")
    void testJobDependencyChaining() {
        // 1. Enqueue parent Job A
        CreateJobRequest reqA = new CreateJobRequest();
        reqA.setJobType("EMAIL_DISPATCH");
        reqA.setPayload("{\"step\": \"A\"}");
        Job jobA = producerService.enqueue(reqA);

        // 2. Enqueue dependent Job B (depends on Job A)
        CreateJobRequest reqB = new CreateJobRequest();
        reqB.setJobType("ORDER_INVOICE");
        reqB.setPayload("{\"step\": \"B\"}");
        reqB.setDependsOnJobId(jobA.getId());
        Job jobB = producerService.enqueue(reqB);

        // 3. Check claim eligibility while Job A is PENDING:
        // Job B must NOT be claimed because Job A is not yet COMPLETED!
        List<String> claimable = jobRepository.claimPendingJobsNative(Instant.now(), 50);
        assertThat(claimable).doesNotContain(jobB.getId().toString());

        // 4. Mark Job A as COMPLETED
        jobA.setStatus(JobStatus.COMPLETED);
        jobRepository.save(jobA);

        // 5. Check claim eligibility again:
        // Now Job B MUST be eligible for claiming!
        List<String> claimableAfterA = jobRepository.claimPendingJobsNative(Instant.now(), 50);
        assertThat(claimableAfterA).contains(jobB.getId().toString());

        System.out.println("Dependency Test PASSED: Verified Job B waited for Job A to complete before becoming claimable.");
    }
}
