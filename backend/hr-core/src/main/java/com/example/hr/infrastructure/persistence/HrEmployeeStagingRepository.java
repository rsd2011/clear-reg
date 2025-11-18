package com.example.hr.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.hr.domain.HrEmployeeStagingEntity;

public interface HrEmployeeStagingRepository extends JpaRepository<HrEmployeeStagingEntity, UUID> {
}
