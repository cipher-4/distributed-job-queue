package com.reliablequeue.repository;

import com.reliablequeue.domain.DeadLetterJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeadLetterJobRepository extends JpaRepository<DeadLetterJob, UUID> {

    Optional<DeadLetterJob> findByOriginalJobId(UUID originalJobId);

    Page<DeadLetterJob> findByReplayedOrderByMovedToDlqAtDesc(boolean replayed, Pageable pageable);

    long countByReplayed(boolean replayed);
}
