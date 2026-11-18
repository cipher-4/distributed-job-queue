package com.reliablequeue.controller;

import com.reliablequeue.domain.DeadLetterJob;
import com.reliablequeue.domain.Job;
import com.reliablequeue.dto.DeadLetterResponse;
import com.reliablequeue.dto.JobResponse;
import com.reliablequeue.service.DeadLetterService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/dlq")
@CrossOrigin(origins = "*")
public class DeadLetterController {

    private final DeadLetterService deadLetterService;

    public DeadLetterController(DeadLetterService deadLetterService) {
        this.deadLetterService = deadLetterService;
    }

    @GetMapping
    public ResponseEntity<Page<DeadLetterResponse>> listDeadLetters(
            @RequestParam(required = false) Boolean replayed,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<DeadLetterJob> dlqPage = deadLetterService.getDeadLetterJobs(replayed, PageRequest.of(page, size));
        return ResponseEntity.ok(dlqPage.map(DeadLetterResponse::fromEntity));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeadLetterResponse> getDeadLetter(@PathVariable UUID id) {
        return deadLetterService.getDeadLetterJob(id)
                .map(dlq -> ResponseEntity.ok(DeadLetterResponse.fromEntity(dlq)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/replay")
    public ResponseEntity<JobResponse> replay(@PathVariable UUID id) {
        Job job = deadLetterService.replayJob(id);
        return ResponseEntity.ok(JobResponse.fromEntity(job));
    }

    @PostMapping("/replay-all")
    public ResponseEntity<Map<String, Object>> replayAll() {
        int count = deadLetterService.replayAll();
        return ResponseEntity.ok(Map.of(
                "replayedCount", count,
                "message", "Successfully queued " + count + " DLQ jobs for execution"
        ));
    }
}
