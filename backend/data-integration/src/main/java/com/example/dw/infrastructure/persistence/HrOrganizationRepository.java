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

    /** 특정 직원이 리더인 활성 조직을 조회한다 (JIT Provisioning용). */
    Optional<HrOrganizationEntity> findFirstByLeaderEmployeeIdAndEffectiveEndIsNullOrderByVersionDesc(String employeeId);

    /** 특정 직원이 업무 매니저인 활성 조직을 조회한다 (JIT Provisioning용). */
    Optional<HrOrganizationEntity> findFirstByManagerEmployeeIdAndEffectiveEndIsNullOrderByVersionDesc(String employeeId);
}
