package com.reliablequeue.domain;

public enum JobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    DEAD_LETTER
}
