package com.example.hr.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.hr.domain.HrOrganizationStagingEntity;

public interface HrOrganizationStagingRepository extends JpaRepository<HrOrganizationStagingEntity, UUID> {
}
