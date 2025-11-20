package com.example.dw.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dw.domain.HrOrganizationEntity;

public interface HrOrganizationRepository extends JpaRepository<HrOrganizationEntity, UUID> {

    Optional<HrOrganizationEntity> findFirstByOrganizationCodeAndEffectiveEndIsNullOrderByVersionDesc(String organizationCode);

    Page<HrOrganizationEntity> findByOrganizationCode(String organizationCode, Pageable pageable);

    Page<HrOrganizationEntity> findByOrganizationCodeOrParentOrganizationCode(String organizationCode,
                                                                               String parentOrganizationCode,
                                                                               Pageable pageable);
}
