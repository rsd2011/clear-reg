package com.example.dw.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dw.domain.HrEmployeeEntity;

public interface HrEmployeeRepository extends JpaRepository<HrEmployeeEntity, UUID> {

    Optional<HrEmployeeEntity> findFirstByEmployeeIdAndEffectiveEndIsNullOrderByVersionDesc(String employeeId);

    default Optional<HrEmployeeEntity> findActive(String employeeId) {
        return findFirstByEmployeeIdAndEffectiveEndIsNullOrderByVersionDesc(employeeId);
    }
}
