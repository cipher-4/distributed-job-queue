package com.reliablequeue.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "queue")
public class QueueProperties {

    private String nodeMode = "ALL_IN_ONE"; // ALL_IN_ONE, PRODUCER, WORKER
    private Worker worker = new Worker();
    private Retry retry = new Retry();

    public static class Worker {
        private boolean enabled = true;
        private String workerId = "worker-default";
        private int concurrency = 4;
        private int batchSize = 5;
        private long pollIntervalMs = 1000;
        private long leaseDurationSeconds = 30;
        private long heartbeatIntervalMs = 10000;
        private long zombieReaperIntervalMs = 10000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getWorkerId() {
            return workerId;
        }

        public void setWorkerId(String workerId) {
            this.workerId = workerId;
        }

        public int getConcurrency() {
            return concurrency;
        }

        public void setConcurrency(int concurrency) {
            this.concurrency = concurrency;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public long getPollIntervalMs() {
            return pollIntervalMs;
        }

        public void setPollIntervalMs(long pollIntervalMs) {
            this.pollIntervalMs = pollIntervalMs;
        }

        public long getLeaseDurationSeconds() {
            return leaseDurationSeconds;
        }

        public void setLeaseDurationSeconds(long leaseDurationSeconds) {
            this.leaseDurationSeconds = leaseDurationSeconds;
        }

        public long getHeartbeatIntervalMs() {
            return heartbeatIntervalMs;
        }

        public void setHeartbeatIntervalMs(long heartbeatIntervalMs) {
            this.heartbeatIntervalMs = heartbeatIntervalMs;
        }

        public long getZombieReaperIntervalMs() {
            return zombieReaperIntervalMs;
        }

        public void setZombieReaperIntervalMs(long zombieReaperIntervalMs) {
            this.zombieReaperIntervalMs = zombieReaperIntervalMs;
        }
    }

    public static class Retry {
        private int defaultMaxRetries = 3;
        private int defaultBackoffBaseSeconds = 2;

        public int getDefaultMaxRetries() {
            return defaultMaxRetries;
        }

        public void setDefaultMaxRetries(int defaultMaxRetries) {
            this.defaultMaxRetries = defaultMaxRetries;
        }

        public int getDefaultBackoffBaseSeconds() {
            return defaultBackoffBaseSeconds;
        }

        public void setDefaultBackoffBaseSeconds(int defaultBackoffBaseSeconds) {
            this.defaultBackoffBaseSeconds = defaultBackoffBaseSeconds;
        }
    }

    public String getNodeMode() {
        return nodeMode;
    }

    public void setNodeMode(String nodeMode) {
        this.nodeMode = nodeMode;
    }

    public Worker getWorker() {
        return worker;
    }

    public void setWorker(Worker worker) {
        this.worker = worker;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }
}
