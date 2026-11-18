package com.reliablequeue.handler.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reliablequeue.domain.Job;
import com.reliablequeue.handler.JobHandler;
import com.reliablequeue.service.IdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentProcessorHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessorHandler.class);
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public PaymentProcessorHandler(IdempotencyService idempotencyService, ObjectMapper objectMapper) {
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getJobType() {
        return "PAYMENT_PROCESS";
    }

    @Override
    public void handle(Job job) throws Exception {
        String idempotencyKey = job.getIdempotencyKey();

        // 1. Guard against duplicate execution (Core Requirement 07)
        if (idempotencyKey != null && idempotencyService.isAlreadyProcessed(idempotencyKey)) {
            log.info("[IDEMPOTENCY GUARD] Payment for job {} with key {} was ALREADY processed. Skipping charge side-effect.",
                    job.getId(), idempotencyKey);
            return;
        }

        JsonNode json = null;
        try {
            json = objectMapper.readTree(job.getPayload());
        } catch (Exception e) {
            log.warn("Payload is not JSON, processing raw text: {}", job.getPayload());
        }

        String accountId = (json != null && json.has("accountId")) ? json.get("accountId").asText() : "acc_default";
        double amount = (json != null && json.has("amount")) ? json.get("amount").asDouble() : 99.99;

        log.info("[PAYMENT] Processing charge of ${} for account {} on job {}", amount, accountId, job.getId());

        // Simulate gateway latency
        Thread.sleep(150);

        // Record successful side effect
        if (idempotencyKey != null) {
            String result = String.format("{\"chargeId\": \"%s\", \"amount\": %.2f, \"status\": \"PAID\"}",
                    UUID.randomUUID(), amount);
            idempotencyService.recordExecution(idempotencyKey, "PAYMENT", job.getId(), "SUCCESS", result);
        }

        log.info("[PAYMENT] Payment successful for job {}", job.getId());
    }
}
