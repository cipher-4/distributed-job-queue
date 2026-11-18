package com.reliablequeue.controller;

import com.reliablequeue.dto.QueueStatsResponse;
import com.reliablequeue.handler.JobHandlerRegistry;
import com.reliablequeue.service.QueueMetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/queue")
@CrossOrigin(origins = "*")
public class QueueMetricsController {

    private final QueueMetricsService metricsService;
    private final JobHandlerRegistry handlerRegistry;

    public QueueMetricsController(QueueMetricsService metricsService, JobHandlerRegistry handlerRegistry) {
        this.metricsService = metricsService;
        this.handlerRegistry = handlerRegistry;
    }

    @GetMapping("/stats")
    public ResponseEntity<QueueStatsResponse> getStats() {
        return ResponseEntity.ok(metricsService.getQueueStats());
    }

    @GetMapping("/handlers")
    public ResponseEntity<Set<String>> getRegisteredHandlers() {
        return ResponseEntity.ok(handlerRegistry.getAllHandlers().keySet());
    }
}
