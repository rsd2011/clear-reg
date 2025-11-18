package com.example.hr.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.hr.domain.HrExternalFeedEntity;
import com.example.hr.domain.HrExternalFeedStatus;

public interface HrExternalFeedRepository extends JpaRepository<HrExternalFeedEntity, UUID> {

    Optional<HrExternalFeedEntity> findFirstByStatusOrderByCreatedAtAsc(HrExternalFeedStatus status);
}
