package com.reliablequeue.handler.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reliablequeue.domain.Job;
import com.reliablequeue.handler.JobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailDispatchHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(EmailDispatchHandler.class);
    private final ObjectMapper objectMapper;

    public EmailDispatchHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getJobType() {
        return "EMAIL_DISPATCH";
    }

    @Override
    public void handle(Job job) throws Exception {
        JsonNode json = null;
        try {
            json = objectMapper.readTree(job.getPayload());
        } catch (Exception ignored) {
        }

        String recipient = (json != null && json.has("to")) ? json.get("to").asText() : "customer@example.com";
        String subject = (json != null && json.has("subject")) ? json.get("subject").asText() : "Order Confirmation";

        log.info("[EMAIL] Sending email '{}' to {} (Job ID: {})", subject, recipient, job.getId());
        Thread.sleep(100);
        log.info("[EMAIL] Email dispatched successfully to {}", recipient);
    }
}
