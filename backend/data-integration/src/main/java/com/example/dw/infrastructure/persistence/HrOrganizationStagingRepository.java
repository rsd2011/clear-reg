package com.example.dw.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dw.domain.HrOrganizationStagingEntity;

public interface HrOrganizationStagingRepository extends JpaRepository<HrOrganizationStagingEntity, UUID> {
}
