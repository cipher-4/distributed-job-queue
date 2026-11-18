package com.reliablequeue.repository;

import com.reliablequeue.domain.IdempotentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotentRecordRepository extends JpaRepository<IdempotentRecord, String> {

    Optional<IdempotentRecord> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
