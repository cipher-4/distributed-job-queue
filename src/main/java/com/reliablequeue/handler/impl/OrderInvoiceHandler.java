package com.reliablequeue.handler.impl;

import com.reliablequeue.domain.Job;
import com.reliablequeue.handler.JobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OrderInvoiceHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderInvoiceHandler.class);

    @Override
    public String getJobType() {
        return "ORDER_INVOICE";
    }

    @Override
    public void handle(Job job) throws Exception {
        log.info("[INVOICE] Generating PDF invoice for payload: {} (Job ID: {})", job.getPayload(), job.getId());
        Thread.sleep(120);
        log.info("[INVOICE] Invoice successfully created and archived for Job ID: {}", job.getId());
    }
}
