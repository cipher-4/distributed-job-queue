package com.reliablequeue.service;

import com.reliablequeue.config.QueueProperties;
import com.reliablequeue.domain.Job;
import com.reliablequeue.domain.JobExecutionLog;
import com.reliablequeue.domain.JobStatus;
import com.reliablequeue.handler.JobHandler;
import com.reliablequeue.handler.JobHandlerRegistry;
import com.reliablequeue.repository.JobExecutionLogRepository;
import com.reliablequeue.repository.JobRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class JobConsumerService {

    private static final Logger log = LoggerFactory.getLogger(JobConsumerService.class);

    private final JobRepository jobRepository;
    private final JobExecutionLogRepository logRepository;
    private final DeadLetterService deadLetterService;
    private final JobHandlerRegistry handlerRegistry;
    private final QueueProperties queueProperties;
    private final TransactionTemplate transactionTemplate;

    private ScheduledExecutorService pollScheduler;
    private ScheduledExecutorService heartbeatScheduler;
    private ExecutorService workerThreadPool;
    private final Set<UUID> activeJobIds = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean running = new AtomicBoolean(false);

    public JobConsumerService(
            JobRepository jobRepository,
            JobExecutionLogRepository logRepository,
            DeadLetterService deadLetterService,
            JobHandlerRegistry handlerRegistry,
            QueueProperties queueProperties,
            PlatformTransactionManager transactionManager
    ) {
        this.jobRepository = jobRepository;
        this.logRepository = logRepository;
        this.deadLetterService = deadLetterService;
        this.handlerRegistry = handlerRegistry;
        this.queueProperties = queueProperties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @PostConstruct
    public void start() {
        String mode = queueProperties.getNodeMode();
        if ("PRODUCER".equalsIgnoreCase(mode) || !queueProperties.getWorker().isEnabled()) {
            log.info("Worker is DISABLED on this node (node-mode: {})", mode);
            return;
        }

        this.pollScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "job-poller");
            t.setDaemon(true);
            return t;
        });

        this.heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "job-heartbeat");
            t.setDaemon(true);
            return t;
        });

        int concurrency = queueProperties.getWorker().getConcurrency();
        this.workerThreadPool = Executors.newFixedThreadPool(concurrency, new ThreadFactory() {
            private int counter = 1;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, queueProperties.getWorker().getWorkerId() + "-worker-" + counter++);
                t.setDaemon(true);
                return t;
            }
        });

        running.set(true);

        long pollInterval = queueProperties.getWorker().getPollIntervalMs();
        pollScheduler.scheduleWithFixedDelay(this::pollAndProcess, 500, pollInterval, TimeUnit.MILLISECONDS);

        long hbInterval = queueProperties.getWorker().getHeartbeatIntervalMs();
        heartbeatScheduler.scheduleWithFixedDelay(this::sendHeartbeats, hbInterval, hbInterval, TimeUnit.MILLISECONDS);

        long zombieInterval = queueProperties.getWorker().getZombieReaperIntervalMs();
        pollScheduler.scheduleWithFixedDelay(this::reapZombies, 2000, zombieInterval, TimeUnit.MILLISECONDS);

        log.info("Job Consumer STARTED: workerId={}, concurrency={}, pollInterval={}ms, lease={}s",
                queueProperties.getWorker().getWorkerId(), concurrency, pollInterval,
                queueProperties.getWorker().getLeaseDurationSeconds());
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        if (pollScheduler != null) {
            pollScheduler.shutdown();
        }
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdown();
        }
        if (workerThreadPool != null) {
            workerThreadPool.shutdown();
            try {
                if (!workerThreadPool.awaitTermination(3, TimeUnit.SECONDS)) {
                    workerThreadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                workerThreadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("Job Consumer STOPPED safely.");
    }

    /**
     * Polling cycle: claims pending jobs using PostgreSQL FOR UPDATE SKIP LOCKED
     */
    public void pollAndProcess() {
        if (!running.get()) {
            return;
        }

        try {
            List<Job> claimedJobs = claimJobsAtomically();
            if (claimedJobs.isEmpty()) {
                return;
            }

            for (Job job : claimedJobs) {
                activeJobIds.add(job.getId());
                workerThreadPool.submit(() -> processJob(job));
            }
        } catch (Exception e) {
            log.error("Error during poll cycle: {}", e.getMessage(), e);
        }
    }

    /**
     * Executes atomic claim inside a dedicated transaction template.
     * Prevents race conditions across multiple worker processes or threads.
     */
    public List<Job> claimJobsAtomically() {
        return transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            int batchSize = queueProperties.getWorker().getBatchSize();
            String workerId = queueProperties.getWorker().getWorkerId();
            long leaseDuration = queueProperties.getWorker().getLeaseDurationSeconds();
            Instant leaseExpires = now.plusSeconds(leaseDuration);

            List<String> claimedIds = jobRepository.claimPendingJobsNative(now, batchSize);
            if (claimedIds.isEmpty()) {
                return Collections.emptyList();
            }

            List<Job> result = new ArrayList<>();
            for (String idStr : claimedIds) {
                UUID id = UUID.fromString(idStr);
                Optional<Job> opt = jobRepository.findById(id);
                if (opt.isPresent()) {
                    Job job = opt.get();
                    job.setStatus(JobStatus.RUNNING);
                    job.setClaimedAt(now);
                    job.setClaimedBy(workerId);
                    job.setLeaseExpiresAt(leaseExpires);
                    job.setLastHeartbeatAt(now);
                    jobRepository.save(job);
                    result.add(job);
                }
            }

            log.info("Worker '{}' claimed {} job(s) via SKIP LOCKED: {}",
                    workerId, result.size(), claimedIds);
            return result;
        });
    }

    /**
     * Worker execution thread for a claimed job
     */
    private void processJob(Job job) {
        Instant startTime = Instant.now();
        String workerId = queueProperties.getWorker().getWorkerId();
        int attempt = job.getRetryCount() + 1;

        log.info("Worker '{}' started executing job {} (Type: {}, Attempt: {})",
                workerId, job.getId(), job.getJobType(), attempt);

        try {
            Optional<JobHandler> handlerOpt = handlerRegistry.getHandler(job.getJobType());
            if (handlerOpt.isEmpty()) {
                throw new IllegalStateException("No registered JobHandler for type: " + job.getJobType());
            }

            // Execute handler
            handlerOpt.get().handle(job);

            // Record success
            handleSuccess(job.getId(), workerId, attempt, startTime);

        } catch (Throwable t) {
            log.error("Job {} failed during attempt {}: {}", job.getId(), attempt, t.getMessage());
            handleFailure(job.getId(), workerId, attempt, startTime, t);
        } finally {
            activeJobIds.remove(job.getId());
        }
    }

    private void handleSuccess(UUID jobId, String workerId, int attempt, Instant startTime) {
        transactionTemplate.executeWithoutResult(status -> {
            Instant finishTime = Instant.now();
            long durationMs = Duration.between(startTime, finishTime).toMillis();

            Optional<Job> opt = jobRepository.findById(jobId);
            if (opt.isPresent()) {
                Job job = opt.get();
                job.setStatus(JobStatus.COMPLETED);
                job.setCompletedAt(finishTime);
                job.setLastError(null);
                job.setLeaseExpiresAt(null);
                jobRepository.save(job);
            }

            JobExecutionLog execLog = new JobExecutionLog(
                    jobId, attempt, workerId, "SUCCESS", startTime, finishTime, durationMs, null
            );
            logRepository.save(execLog);

            log.info("Job {} COMPLETED successfully in {}ms (Attempt {})", jobId, durationMs, attempt);
        });
    }

    private void handleFailure(UUID jobId, String workerId, int attempt, Instant startTime, Throwable error) {
        transactionTemplate.executeWithoutResult(status -> {
            Instant finishTime = Instant.now();
            long durationMs = Duration.between(startTime, finishTime).toMillis();
            String errorMsg = (error != null) ? error.getMessage() : "Unknown execution failure";

            Optional<Job> opt = jobRepository.findById(jobId);
            if (opt.isEmpty()) {
                return;
            }
            Job job = opt.get();
            int newRetryCount = job.getRetryCount() + 1;
            job.setRetryCount(newRetryCount);
            job.setLastError(errorMsg);

            if (newRetryCount >= job.getMaxRetries()) {
                // Max retries exceeded -> Dead Letter Queue
                job.setStatus(JobStatus.DEAD_LETTER);
                job.setFailedAt(finishTime);
                job.setLeaseExpiresAt(null);
                jobRepository.save(job);

                deadLetterService.archiveToDeadLetter(job, error);

                JobExecutionLog execLog = new JobExecutionLog(
                        jobId, attempt, workerId, "DEAD_LETTER", startTime, finishTime, durationMs, errorMsg
                );
                logRepository.save(execLog);

                log.warn("Job {} EXCEEDED max retries ({}/{}). MOVED TO DEAD-LETTER QUEUE.",
                        jobId, newRetryCount, job.getMaxRetries());

            } else {
                // Exponential backoff: base * (2 ^ (retryCount - 1))
                long backoffSeconds = (long) (job.getBackoffBaseSeconds() * Math.pow(2, newRetryCount - 1));
                Instant nextRun = Instant.now().plusSeconds(backoffSeconds);

                job.setStatus(JobStatus.PENDING);
                job.setScheduledAt(nextRun);
                job.setClaimedBy(null);
                job.setClaimedAt(null);
                job.setLeaseExpiresAt(null);
                jobRepository.save(job);

                JobExecutionLog execLog = new JobExecutionLog(
                        jobId, attempt, workerId, "RETRY_SCHEDULED", startTime, finishTime, durationMs, errorMsg
                );
                logRepository.save(execLog);

                log.info("Job {} scheduled for retry #{}/{} in {}s at {}",
                        jobId, newRetryCount, job.getMaxRetries(), backoffSeconds, nextRun);
            }
        });
    }

    /**
     * Heartbeat task: extends lease for currently running jobs
     */
    private void sendHeartbeats() {
        if (!running.get() || activeJobIds.isEmpty()) {
            return;
        }

        String workerId = queueProperties.getWorker().getWorkerId();
        Instant now = Instant.now();
        Instant newExpiry = now.plusSeconds(queueProperties.getWorker().getLeaseDurationSeconds());

        for (UUID jobId : activeJobIds) {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    jobRepository.renewLease(jobId, workerId, now, newExpiry);
                });
                log.debug("Renewed lease for job {} until {}", jobId, newExpiry);
            } catch (Exception e) {
                log.warn("Failed to renew lease for job {}: {}", jobId, e.getMessage());
            }
        }
    }

    /**
     * Zombie Reaper: recovers jobs whose worker died mid-flight (lease expired)
     */
    public void reapZombies() {
        if (!running.get()) {
            return;
        }

        try {
            transactionTemplate.executeWithoutResult(status -> {
                Instant now = Instant.now();
                int batchSize = queueProperties.getWorker().getBatchSize();
                List<String> expiredIds = jobRepository.claimExpiredLeasesNative(now, batchSize);

                if (expiredIds.isEmpty()) {
                    return;
                }

                log.warn("[ZOMBIE REAPER] Detected {} expired worker lease(s): {}", expiredIds.size(), expiredIds);

                for (String idStr : expiredIds) {
                    UUID jobId = UUID.fromString(idStr);
                    Optional<Job> opt = jobRepository.findById(jobId);
                    if (opt.isPresent()) {
                        Job job = opt.get();
                        String prevWorker = job.getClaimedBy();
                        int newRetries = job.getRetryCount() + 1;
                        job.setRetryCount(newRetries);

                        String crashMsg = String.format("Worker lease expired (node '%s' crashed or timed out)", prevWorker);
                        job.setLastError(crashMsg);

                        if (newRetries >= job.getMaxRetries()) {
                            job.setStatus(JobStatus.DEAD_LETTER);
                            job.setFailedAt(now);
                            job.setLeaseExpiresAt(null);
                            jobRepository.save(job);
                            deadLetterService.archiveToDeadLetter(job, new RuntimeException(crashMsg));
                        } else {
                            long backoffSeconds = (long) (job.getBackoffBaseSeconds() * Math.pow(2, newRetries - 1));
                            job.setStatus(JobStatus.PENDING);
                            job.setScheduledAt(now.plusSeconds(backoffSeconds));
                            job.setClaimedBy(null);
                            job.setLeaseExpiresAt(null);
                            jobRepository.save(job);
                            log.info("[ZOMBIE RECOVERED] Job {} reclaimed from crashed worker '{}', retrying in {}s",
                                    jobId, prevWorker, backoffSeconds);
                        }
                    }
                }
            });
        } catch (Exception e) {
            log.error("Error in zombie reaper cycle: {}", e.getMessage(), e);
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public int getActiveJobCount() {
        return activeJobIds.size();
    }
}
