package com.reliablequeue.repository;

import com.reliablequeue.domain.Job;
import com.reliablequeue.domain.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    Optional<Job> findByIdempotencyKey(String idempotencyKey);

    Page<Job> findByStatusOrderByCreatedAtDesc(JobStatus status, Pageable pageable);

    Page<Job> findByQueueNameOrderByCreatedAtDesc(String queueName, Pageable pageable);

    long countByStatus(JobStatus status);

    long countByStatusAndScheduledAtAfter(JobStatus status, Instant now);

    @Query(value = """
        SELECT CAST(j.id AS VARCHAR) FROM jobs j
        WHERE j.status = 'PENDING'
          AND j.scheduled_at <= :now
          AND (
            j.depends_on_job_id IS NULL
            OR EXISTS (
                SELECT 1 FROM jobs parent
                WHERE parent.id = j.depends_on_job_id
                  AND parent.status = 'COMPLETED'
            )
          )
        ORDER BY j.priority DESC, j.scheduled_at ASC, j.created_at ASC
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<String> claimPendingJobsNative(
            @Param("now") Instant now,
            @Param("limit") int limit
    );

    @Query(value = """
        SELECT CAST(j.id AS VARCHAR) FROM jobs j
        WHERE j.status = 'RUNNING'
          AND j.lease_expires_at < :now
        ORDER BY j.lease_expires_at ASC
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<String> claimExpiredLeasesNative(
            @Param("now") Instant now,
            @Param("limit") int limit
    );

    @Modifying
    @Query("UPDATE Job j SET j.lastHeartbeatAt = :now, j.leaseExpiresAt = :newExpiresAt WHERE j.id = :id AND j.claimedBy = :workerId AND j.status = com.reliablequeue.domain.JobStatus.RUNNING")
    int renewLease(
            @Param("id") UUID id,
            @Param("workerId") String workerId,
            @Param("now") Instant now,
            @Param("newExpiresAt") Instant newExpiresAt
    );
}
