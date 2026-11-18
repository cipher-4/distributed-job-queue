package com.reliablequeue.handler;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JobHandlerRegistry {

    private final Map<String, JobHandler> handlers = new ConcurrentHashMap<>();

    public JobHandlerRegistry(List<JobHandler> handlerList) {
        for (JobHandler handler : handlerList) {
            handlers.put(handler.getJobType().toUpperCase(), handler);
        }
    }

    public Optional<JobHandler> getHandler(String jobType) {
        if (jobType == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(handlers.get(jobType.toUpperCase()));
    }

    public Map<String, JobHandler> getAllHandlers() {
        return handlers;
    }
}
