package com.example.hr.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.hr.domain.HrOrganizationEntity;

public interface HrOrganizationRepository extends JpaRepository<HrOrganizationEntity, UUID> {

    Optional<HrOrganizationEntity> findFirstByOrganizationCodeAndEffectiveEndIsNullOrderByVersionDesc(String organizationCode);
}
