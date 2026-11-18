package com.reliablequeue.dto;

import java.util.Map;

public class QueueStatsResponse {

    private long totalJobs;
    private long pendingJobs;
    private long delayedScheduledJobs;
    private long runningJobs;
    private long completedJobs;
    private long failedJobs;
    private long deadLetterJobs;
    private long unreplayedDlqJobs;
    private String nodeMode;
    private boolean workerActive;
    private int workerConcurrency;
    private String workerId;
    private Map<String, Long> statusBreakdown;

    public long getTotalJobs() {
        return totalJobs;
    }

    public void setTotalJobs(long totalJobs) {
        this.totalJobs = totalJobs;
    }

    public long getPendingJobs() {
        return pendingJobs;
    }

    public void setPendingJobs(long pendingJobs) {
        this.pendingJobs = pendingJobs;
    }

    public long getDelayedScheduledJobs() {
        return delayedScheduledJobs;
    }

    public void setDelayedScheduledJobs(long delayedScheduledJobs) {
        this.delayedScheduledJobs = delayedScheduledJobs;
    }

    public long getRunningJobs() {
        return runningJobs;
    }

    public void setRunningJobs(long runningJobs) {
        this.runningJobs = runningJobs;
    }

    public long getCompletedJobs() {
        return completedJobs;
    }

    public void setCompletedJobs(long completedJobs) {
        this.completedJobs = completedJobs;
    }

    public long getFailedJobs() {
        return failedJobs;
    }

    public void setFailedJobs(long failedJobs) {
        this.failedJobs = failedJobs;
    }

    public long getDeadLetterJobs() {
        return deadLetterJobs;
    }

    public void setDeadLetterJobs(long deadLetterJobs) {
        this.deadLetterJobs = deadLetterJobs;
    }

    public long getUnreplayedDlqJobs() {
        return unreplayedDlqJobs;
    }

    public void setUnreplayedDlqJobs(long unreplayedDlqJobs) {
        this.unreplayedDlqJobs = unreplayedDlqJobs;
    }

    public String getNodeMode() {
        return nodeMode;
    }

    public void setNodeMode(String nodeMode) {
        this.nodeMode = nodeMode;
    }

    public boolean isWorkerActive() {
        return workerActive;
    }

    public void setWorkerActive(boolean workerActive) {
        this.workerActive = workerActive;
    }

    public int getWorkerConcurrency() {
        return workerConcurrency;
    }

    public void setWorkerConcurrency(int workerConcurrency) {
        this.workerConcurrency = workerConcurrency;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public Map<String, Long> getStatusBreakdown() {
        return statusBreakdown;
    }

    public void setStatusBreakdown(Map<String, Long> statusBreakdown) {
        this.statusBreakdown = statusBreakdown;
    }
}
