package com.example.dw.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dw.domain.DwCommonCodeEntity;

public interface DwCommonCodeRepository extends JpaRepository<DwCommonCodeEntity, UUID> {

    Optional<DwCommonCodeEntity> findFirstByCodeTypeAndCodeValue(String codeType, String codeValue);

    List<DwCommonCodeEntity> findByCodeTypeAndActiveTrueOrderByDisplayOrderAscCodeValueAsc(String codeType);
}
