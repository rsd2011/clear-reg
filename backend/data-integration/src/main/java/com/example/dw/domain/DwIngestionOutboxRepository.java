package com.example.dw.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.example.dw.application.job.DwIngestionOutboxStatus;

import jakarta.persistence.LockModeType;

public interface DwIngestionOutboxRepository extends JpaRepository<DwIngestionOutbox, java.util.UUID> {

    List<DwIngestionOutbox> findTop50ByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
            DwIngestionOutboxStatus status,
            OffsetDateTime availableAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from DwIngestionOutbox o where o.status = :status and o.availableAt <= :availableAt order by o.createdAt asc")
    List<DwIngestionOutbox> lockOldestPending(@org.springframework.data.repository.query.Param("status") DwIngestionOutboxStatus status,
                                              @org.springframework.data.repository.query.Param("availableAt") OffsetDateTime availableAt,
                                              Pageable pageable);
}
