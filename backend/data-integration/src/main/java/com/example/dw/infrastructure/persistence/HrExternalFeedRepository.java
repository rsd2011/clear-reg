package com.example.dw.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dw.domain.HrExternalFeedEntity;
import com.example.dw.domain.HrExternalFeedStatus;

public interface HrExternalFeedRepository extends JpaRepository<HrExternalFeedEntity, UUID> {

    Optional<HrExternalFeedEntity> findFirstByStatusOrderByCreatedAtAsc(HrExternalFeedStatus status);
}
