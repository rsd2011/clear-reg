package com.example.admin.systemconfig.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.admin.systemconfig.domain.SystemConfigRevision;

/**
 * 시스템 설정 리비전 Repository.
 */
public interface SystemConfigRevisionRepository extends JpaRepository<SystemConfigRevision, UUID> {

  /**
   * 현재 유효한 게시 버전 조회 (valid_to IS NULL AND status = PUBLISHED).
   */
  @Query("""
          SELECT r FROM SystemConfigRevision r
          WHERE r.root.id = :rootId
            AND r.validTo IS NULL
            AND r.status = com.example.common.version.VersionStatus.PUBLISHED
          """)
  Optional<SystemConfigRevision> findCurrentByRootId(@Param("rootId") UUID rootId);

  /**
   * 특정 시점의 버전 조회 (Point-in-Time Query).
   */
  @Query("""
          SELECT r FROM SystemConfigRevision r
          WHERE r.root.id = :rootId
            AND r.validFrom <= :asOf
            AND (r.validTo IS NULL OR r.validTo > :asOf)
            AND r.status != com.example.common.version.VersionStatus.DRAFT
          """)
  Optional<SystemConfigRevision> findByRootIdAsOf(
      @Param("rootId") UUID rootId,
      @Param("asOf") OffsetDateTime asOf);

  /**
   * 버전 이력 조회 (최신순, Draft 제외).
   */
  @Query("""
          SELECT r FROM SystemConfigRevision r
          WHERE r.root.id = :rootId
            AND r.status != com.example.common.version.VersionStatus.DRAFT
          ORDER BY r.version DESC
          """)
  List<SystemConfigRevision> findHistoryByRootId(@Param("rootId") UUID rootId);

  /**
   * 버전 이력 조회 (최신순, Draft 포함).
   */
  List<SystemConfigRevision> findByRootIdOrderByVersionDesc(UUID rootId);

  /**
   * 특정 버전 번호로 조회.
   */
  Optional<SystemConfigRevision> findByRootIdAndVersion(UUID rootId, Integer version);

  /**
   * 최신 버전 번호 조회.
   */
  @Query("SELECT COALESCE(MAX(r.version), 0) FROM SystemConfigRevision r WHERE r.root.id = :rootId")
  int findMaxVersionByRootId(@Param("rootId") UUID rootId);

  /**
   * 이력 개수 조회 (Draft 제외).
   */
  @Query("""
          SELECT COUNT(r) FROM SystemConfigRevision r
          WHERE r.root.id = :rootId
            AND r.status != com.example.common.version.VersionStatus.DRAFT
          """)
  long countHistoryByRootId(@Param("rootId") UUID rootId);

  /**
   * 초안 버전 조회 (설정당 최대 1개).
   */
  @Query("""
          SELECT r FROM SystemConfigRevision r
          WHERE r.root.id = :rootId
            AND r.status = com.example.common.version.VersionStatus.DRAFT
          """)
  Optional<SystemConfigRevision> findDraftByRootId(@Param("rootId") UUID rootId);

  /**
   * 초안이 있는지 확인.
   */
  @Query("""
          SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
          FROM SystemConfigRevision r
          WHERE r.root.id = :rootId
            AND r.status = com.example.common.version.VersionStatus.DRAFT
          """)
  boolean existsDraftByRootId(@Param("rootId") UUID rootId);

  /**
   * 두 버전 비교를 위한 조회.
   */
  @Query("""
          SELECT r FROM SystemConfigRevision r
          WHERE r.root.id = :rootId
            AND r.version IN (:version1, :version2)
          ORDER BY r.version ASC
          """)
  List<SystemConfigRevision> findVersionsForComparison(
      @Param("rootId") UUID rootId,
      @Param("version1") Integer version1,
      @Param("version2") Integer version2);
}
