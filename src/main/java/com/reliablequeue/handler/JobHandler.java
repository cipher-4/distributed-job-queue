package com.reliablequeue.handler;

import com.reliablequeue.domain.Job;

public interface JobHandler {

    /**
     * The unique job type identifier this handler processes
     */
    String getJobType();

    /**
     * Executes the job business logic.
     * Throws exception on failure to trigger automatic exponential backoff retry.
     */
    void handle(Job job) throws Exception;
}
