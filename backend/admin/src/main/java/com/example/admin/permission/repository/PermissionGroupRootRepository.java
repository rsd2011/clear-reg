package com.example.admin.permission.repository;

import com.example.admin.permission.domain.PermissionGroupRoot;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 권한 그룹 루트 Repository.
 */
public interface PermissionGroupRootRepository extends JpaRepository<PermissionGroupRoot, UUID> {

  /**
   * 그룹 코드로 조회.
   */
  Optional<PermissionGroupRoot> findByGroupCode(String groupCode);

  /**
   * 그룹 코드 존재 여부 확인.
   */
  boolean existsByGroupCode(String groupCode);

  /**
   * ID로 조회하면서 현재 활성 버전이 active 상태인 루트만 반환합니다.
   */
  @Query("""
          SELECT r FROM PermissionGroupRoot r
          WHERE r.id = :id
            AND r.currentVersion IS NOT NULL
            AND r.currentVersion.active = true
          """)
  Optional<PermissionGroupRoot> findByIdAndActiveVersion(@Param("id") UUID id);

  /**
   * 그룹 코드로 조회하면서 현재 버전을 fetch join합니다.
   */
  @Query("""
          SELECT r FROM PermissionGroupRoot r
          LEFT JOIN FETCH r.currentVersion
          WHERE r.groupCode = :groupCode
          """)
  Optional<PermissionGroupRoot> findByGroupCodeWithCurrentVersion(@Param("groupCode") String groupCode);
}
