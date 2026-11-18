package com.reliablequeue.service;

import com.reliablequeue.domain.Job;
import com.reliablequeue.dto.CreateJobRequest;
import com.reliablequeue.repository.JobRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"queue.worker.enabled=false"})
public class JobClaimConcurrencyTest {

    @Autowired
    private JobProducerService producerService;

    @Autowired
    private JobConsumerService consumerService;

    @Autowired
    private JobRepository jobRepository;

    @Test
    @DisplayName("Verify SKIP LOCKED prevents race conditions when 10 threads claim concurrently")
    void testConcurrentJobClaimsWithoutDuplication() throws InterruptedException, ExecutionException {
        // 1. Enqueue 30 pending jobs
        int jobCount = 30;
        List<UUID> enqueuedIds = new ArrayList<>();
        for (int i = 0; i < jobCount; i++) {
            CreateJobRequest req = new CreateJobRequest();
            req.setJobType("EMAIL_DISPATCH");
            req.setPayload("{\"testId\": " + i + "}");
            req.setPriority((i % 10) + 1);
            Job job = producerService.enqueue(req);
            enqueuedIds.add(job.getId());
        }

        // 2. Concurrently call claimJobsAtomically() across 10 threads
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Callable<List<Job>>> tasks = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            tasks.add(() -> consumerService.claimJobsAtomically());
        }

        List<Future<List<Job>>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        // 3. Aggregate all claimed job IDs across all worker threads
        List<UUID> allClaimedIds = new ArrayList<>();
        for (Future<List<Job>> future : futures) {
            List<Job> batch = future.get();
            for (Job j : batch) {
                allClaimedIds.add(j.getId());
            }
        }

        // 4. Verify ZERO duplicates: each claimed job was claimed by exactly ONE thread
        Set<UUID> uniqueClaimed = new HashSet<>(allClaimedIds);
        assertThat(allClaimedIds.size())
                .as("Total claimed jobs must match unique claimed jobs count (no duplicates)")
                .isEqualTo(uniqueClaimed.size());

        System.out.println("Concurrency Test PASSED: Successfully claimed " + uniqueClaimed.size() +
                " jobs across 10 parallel threads with 0 duplicates!");
    }
}
