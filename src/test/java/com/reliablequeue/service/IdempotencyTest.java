package com.reliablequeue.service;

import com.reliablequeue.domain.Job;
import com.reliablequeue.dto.CreateJobRequest;
import com.reliablequeue.repository.IdempotentRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"queue.worker.enabled=false"})
public class IdempotencyTest {

    @Autowired
    private JobProducerService producerService;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private IdempotentRecordRepository idempotentRecordRepository;

    @Test
    @DisplayName("Verify idempotency key prevents duplicate enqueue and duplicate side-effects")
    void testIdempotencyGuard() {
        String testKey = "order-payment-key-" + UUID.randomUUID();

        // 1. First enqueue
        CreateJobRequest req1 = new CreateJobRequest();
        req1.setJobType("PAYMENT_PROCESS");
        req1.setIdempotencyKey(testKey);
        req1.setPayload("{\"amount\": 199.99, \"accountId\": \"acc_test_99\"}");
        Job job1 = producerService.enqueue(req1);

        // 2. Second enqueue with identical idempotencyKey
        CreateJobRequest req2 = new CreateJobRequest();
        req2.setJobType("PAYMENT_PROCESS");
        req2.setIdempotencyKey(testKey);
        req2.setPayload("{\"amount\": 199.99, \"accountId\": \"acc_test_99\"}");
        Job job2 = producerService.enqueue(req2);

        // 3. Must return the exact same job instance ID without creating duplicate
        assertThat(job1.getId()).isEqualTo(job2.getId());

        // 4. Test Idempotency Ledger execution recording
        assertThat(idempotencyService.isAlreadyProcessed(testKey)).isFalse();

        idempotencyService.recordExecution(testKey, "PAYMENT", job1.getId(), "SUCCESS", "{\"status\": \"PAID\"}");

        // Now must report already processed
        assertThat(idempotencyService.isAlreadyProcessed(testKey)).isTrue();
        assertThat(idempotentRecordRepository.existsByIdempotencyKey(testKey)).isTrue();

        System.out.println("Idempotency Test PASSED: Verified duplicate enqueue guard and ledger recording.");
    }
}
