package com.reliablequeue.handler.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reliablequeue.domain.Job;
import com.reliablequeue.handler.JobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimulatedFailureHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(SimulatedFailureHandler.class);
    private final ObjectMapper objectMapper;

    public SimulatedFailureHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getJobType() {
        return "SIMULATED_FAILURE";
    }

    @Override
    public void handle(Job job) throws Exception {
        log.info("[SIMULATION] Executing simulated failure job {} (current retry attempt: {})",
                job.getId(), job.getRetryCount());

        JsonNode json = null;
        try {
            json = objectMapper.readTree(job.getPayload());
        } catch (Exception ignored) {
        }

        int failTimes = (json != null && json.has("failTimes")) ? json.get("failTimes").asInt() : 999;
        boolean alwaysFail = (json != null && json.has("alwaysFail")) ? json.get("alwaysFail").asBoolean() : false;

        if (alwaysFail || job.getRetryCount() < failTimes) {
            String errorMsg = String.format("Simulated network outage/service failure on attempt %d (configured to fail %d times)",
                    job.getRetryCount() + 1, failTimes);
            log.warn("[SIMULATION] Intentionally throwing exception for job {}: {}", job.getId(), errorMsg);
            throw new RuntimeException(errorMsg);
        }

        log.info("[SIMULATION] Job {} succeeded on attempt {} after configured previous failures!",
                job.getId(), job.getRetryCount() + 1);
    }
}
