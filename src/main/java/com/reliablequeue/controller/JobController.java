package com.reliablequeue.controller;

import com.reliablequeue.domain.Job;
import com.reliablequeue.domain.JobExecutionLog;
import com.reliablequeue.domain.JobStatus;
import com.reliablequeue.dto.CreateJobRequest;
import com.reliablequeue.dto.JobResponse;
import com.reliablequeue.repository.JobExecutionLogRepository;
import com.reliablequeue.repository.JobRepository;
import com.reliablequeue.service.JobConsumerService;
import com.reliablequeue.service.JobProducerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*")
public class JobController {

    private final JobProducerService producerService;
    private final JobRepository jobRepository;
    private final JobExecutionLogRepository logRepository;
    private final JobConsumerService consumerService;

    public JobController(
            JobProducerService producerService,
            JobRepository jobRepository,
            JobExecutionLogRepository logRepository,
            JobConsumerService consumerService
    ) {
        this.producerService = producerService;
        this.jobRepository = jobRepository;
        this.logRepository = logRepository;
        this.consumerService = consumerService;
    }

    @PostMapping
    public ResponseEntity<JobResponse> enqueue(@Valid @RequestBody CreateJobRequest request) {
        Job job = producerService.enqueue(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(JobResponse.fromEntity(job));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJob(@PathVariable UUID id) {
        return producerService.getJob(id)
                .map(job -> ResponseEntity.ok(JobResponse.fromEntity(job)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<Page<JobResponse>> listJobs(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) String queue,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Job> jobs;
        if (status != null) {
            jobs = jobRepository.findByStatusOrderByCreatedAtDesc(status, pageRequest);
        } else if (queue != null && !queue.isBlank()) {
            jobs = jobRepository.findByQueueNameOrderByCreatedAtDesc(queue, pageRequest);
        } else {
            jobs = jobRepository.findAll(pageRequest);
        }
        return ResponseEntity.ok(jobs.map(JobResponse::fromEntity));
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<List<JobExecutionLog>> getExecutionLogs(@PathVariable UUID id) {
        List<JobExecutionLog> logs = logRepository.findByJobIdOrderByAttemptNumberAsc(id);
        return ResponseEntity.ok(logs);
    }

    @PostMapping("/trigger-claim")
    public ResponseEntity<String> triggerClaim() {
        consumerService.pollAndProcess();
        return ResponseEntity.ok("Claim cycle triggered");
    }
}
