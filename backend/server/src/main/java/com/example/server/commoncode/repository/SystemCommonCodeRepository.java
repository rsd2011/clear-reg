package com.example.server.commoncode.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.server.commoncode.model.SystemCommonCode;

public interface SystemCommonCodeRepository extends JpaRepository<SystemCommonCode, UUID> {

    List<SystemCommonCode> findByCodeTypeOrderByDisplayOrderAscCodeValueAsc(String codeType);

    Optional<SystemCommonCode> findByCodeTypeAndCodeValue(String codeType, String codeValue);
}
