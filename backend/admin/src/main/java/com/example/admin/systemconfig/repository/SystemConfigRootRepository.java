package com.example.admin.systemconfig.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.admin.systemconfig.domain.SystemConfigRoot;

/**
 * 시스템 설정 루트 Repository.
 */
public interface SystemConfigRootRepository extends JpaRepository<SystemConfigRoot, UUID> {

  /**
   * 설정 코드로 루트 조회.
   */
  Optional<SystemConfigRoot> findByConfigCode(String configCode);

  /**
   * 설정 코드 존재 여부 확인.
   */
  boolean existsByConfigCode(String configCode);
}
