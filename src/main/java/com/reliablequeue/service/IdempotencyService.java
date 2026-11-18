package com.reliablequeue.service;

import com.reliablequeue.domain.IdempotentRecord;
import com.reliablequeue.repository.IdempotentRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotentRecordRepository repository;

    public IdempotencyService(IdempotentRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean isAlreadyProcessed(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return false;
        }
        return repository.findByIdempotencyKey(idempotencyKey)
                .map(r -> "SUCCESS".equalsIgnoreCase(r.getStatus()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Optional<IdempotentRecord> getRecord(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return repository.findByIdempotencyKey(idempotencyKey);
    }

    @Transactional
    public IdempotentRecord recordExecution(String idempotencyKey, String operationType, UUID jobId, String status, String resultData) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }

        Optional<IdempotentRecord> existing = repository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            IdempotentRecord rec = existing.get();
            rec.setStatus(status);
            rec.setResultData(resultData);
            rec.setExecutedAt(Instant.now());
            return repository.save(rec);
        }

        IdempotentRecord record = new IdempotentRecord(idempotencyKey, operationType, jobId, status, resultData);
        log.info("Recorded idempotency key: {} for operation: {} (job: {})", idempotencyKey, operationType, jobId);
        return repository.save(record);
    }
}
