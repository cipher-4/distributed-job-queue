package com.reliablequeue.cli;

import com.reliablequeue.domain.DeadLetterJob;
import com.reliablequeue.domain.Job;
import com.reliablequeue.dto.CreateJobRequest;
import com.reliablequeue.dto.QueueStatsResponse;
import com.reliablequeue.service.DeadLetterService;
import com.reliablequeue.service.JobProducerService;
import com.reliablequeue.service.QueueMetricsService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

@Component
public class QueueCliRunner implements CommandLineRunner {

    private final QueueMetricsService metricsService;
    private final DeadLetterService dlqService;
    private final JobProducerService producerService;

    public QueueCliRunner(QueueMetricsService metricsService, DeadLetterService dlqService, JobProducerService producerService) {
        this.metricsService = metricsService;
        this.dlqService = dlqService;
        this.producerService = producerService;
    }

    @Override
    public void run(String... args) {
        if (args == null || args.length == 0) {
            return;
        }

        for (int i = 0; i < args.length; i++) {
            if ("--cli".equalsIgnoreCase(args[i]) && (i + 1) < args.length) {
                String cmd = args[i + 1].toLowerCase();
                handleCliCommand(cmd, Arrays.copyOfRange(args, i + 2, args.length));
                break;
            }
        }
    }

    private void handleCliCommand(String command, String[] params) {
        System.out.println("=================================================");
        System.out.println("       RELIABLE JOB QUEUE - CLI INSPECTOR        ");
        System.out.println("=================================================");

        switch (command) {
            case "stats", "health" -> {
                QueueStatsResponse stats = metricsService.getQueueStats();
                System.out.println("Node Mode:           " + stats.getNodeMode());
                System.out.println("Worker ID:           " + stats.getWorkerId());
                System.out.println("Worker Active:       " + stats.isWorkerActive() + " (concurrency: " + stats.getWorkerConcurrency() + ")");
                System.out.println("Total Jobs:          " + stats.getTotalJobs());
                System.out.println("Pending (Ready):     " + stats.getPendingJobs());
                System.out.println("Delayed (Scheduled): " + stats.getDelayedScheduledJobs());
                System.out.println("In-Flight (Running): " + stats.getRunningJobs());
                System.out.println("Completed:           " + stats.getCompletedJobs());
                System.out.println("Failed:              " + stats.getFailedJobs());
                System.out.println("Dead-Letter Queue:   " + stats.getDeadLetterJobs() + " (Unreplayed: " + stats.getUnreplayedDlqJobs() + ")");
            }
            case "dlq" -> {
                Page<DeadLetterJob> page = dlqService.getDeadLetterJobs(null, PageRequest.of(0, 10));
                System.out.println("Dead Letter Queue Entries (showing up to 10):");
                if (page.isEmpty()) {
                    System.out.println("No dead-letter items found.");
                } else {
                    for (DeadLetterJob item : page) {
                        System.out.printf("[%s] Type: %s, Retries: %d, Replayed: %s\n  Reason: %s\n",
                                item.getId(), item.getJobType(), item.getRetryCount(), item.isReplayed(), item.getFailureReason());
                    }
                }
            }
            case "replay" -> {
                if (params.length > 0) {
                    UUID id = UUID.fromString(params[0]);
                    Job job = dlqService.replayJob(id);
                    System.out.println("Successfully replayed DLQ item " + id + " as new Job: " + job.getId());
                } else {
                    int count = dlqService.replayAll();
                    System.out.println("Bulk replayed " + count + " DLQ item(s).");
                }
            }
            case "enqueue" -> {
                String type = (params.length > 0) ? params[0] : "PAYMENT_PROCESS";
                String payload = (params.length > 1) ? params[1] : "{\"amount\": 150.00}";
                CreateJobRequest req = new CreateJobRequest();
                req.setJobType(type);
                req.setPayload(payload);
                Job job = producerService.enqueue(req);
                System.out.println("Successfully enqueued job: " + job.getId() + " [Type: " + job.getJobType() + "]");
            }
            default -> {
                System.out.println("Unknown command: " + command);
                System.out.println("Usage: --cli [stats|dlq|replay <id>|enqueue <type> <payload>]");
            }
        }
        System.out.println("=================================================");
    }
}
