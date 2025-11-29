package com.example.admin.maskingpolicy.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.admin.maskingpolicy.domain.MaskingPolicy;

/**
 * 마스킹 정책 버전 Repository.
 */
public interface MaskingPolicyRepository extends JpaRepository<MaskingPolicy, UUID> {

    /**
     * 현재 유효한 게시 버전 조회 (valid_to IS NULL AND status = PUBLISHED).
     */
    @Query("""
            SELECT v FROM MaskingPolicy v
            WHERE v.root.id = :rootId
              AND v.validTo IS NULL
              AND v.status = com.example.common.version.VersionStatus.PUBLISHED
            """)
    Optional<MaskingPolicy> findCurrentByRootId(@Param("rootId") UUID rootId);

    /**
     * 특정 시점의 버전 조회 (시점 조회).
     */
    @Query("""
            SELECT v FROM MaskingPolicy v
            WHERE v.root.id = :rootId
              AND v.validFrom <= :asOf
              AND (v.validTo IS NULL OR v.validTo > :asOf)
              AND v.status != com.example.common.version.VersionStatus.DRAFT
            """)
    Optional<MaskingPolicy> findByRootIdAsOf(
            @Param("rootId") UUID rootId,
            @Param("asOf") OffsetDateTime asOf);

    /**
     * 버전 이력 조회 (최신순, Draft 제외).
     */
    @Query("""
            SELECT v FROM MaskingPolicy v
            WHERE v.root.id = :rootId
              AND v.status != com.example.common.version.VersionStatus.DRAFT
            ORDER BY v.version DESC
            """)
    List<MaskingPolicy> findHistoryByRootId(@Param("rootId") UUID rootId);

    /**
     * 버전 이력 조회 (최신순, Draft 포함).
     */
    List<MaskingPolicy> findByRootIdOrderByVersionDesc(UUID rootId);

    /**
     * 특정 버전 번호로 조회.
     */
    Optional<MaskingPolicy> findByRootIdAndVersion(UUID rootId, Integer version);

    /**
     * 최신 버전 번호 조회.
     */
    @Query("SELECT COALESCE(MAX(v.version), 0) FROM MaskingPolicy v WHERE v.root.id = :rootId")
    int findMaxVersionByRootId(@Param("rootId") UUID rootId);

    /**
     * 이력 개수 조회 (Draft 제외).
     */
    @Query("""
            SELECT COUNT(v) FROM MaskingPolicy v
            WHERE v.root.id = :rootId
              AND v.status != com.example.common.version.VersionStatus.DRAFT
            """)
    long countHistoryByRootId(@Param("rootId") UUID rootId);

    /**
     * 초안 버전 조회 (정책당 최대 1개).
     */
    @Query("""
            SELECT v FROM MaskingPolicy v
            WHERE v.root.id = :rootId
              AND v.status = com.example.common.version.VersionStatus.DRAFT
            """)
    Optional<MaskingPolicy> findDraftByRootId(@Param("rootId") UUID rootId);

    /**
     * 초안이 있는지 확인.
     */
    @Query("""
            SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END
            FROM MaskingPolicy v
            WHERE v.root.id = :rootId
              AND v.status = com.example.common.version.VersionStatus.DRAFT
            """)
    boolean existsDraftByRootId(@Param("rootId") UUID rootId);

    /**
     * 두 버전 비교를 위한 조회.
     */
    @Query("""
            SELECT v FROM MaskingPolicy v
            WHERE v.root.id = :rootId
              AND v.version IN (:version1, :version2)
            ORDER BY v.version ASC
            """)
    List<MaskingPolicy> findVersionsForComparison(
            @Param("rootId") UUID rootId,
            @Param("version1") Integer version1,
            @Param("version2") Integer version2);

    /**
     * 모든 현재 활성 버전 조회 (우선순위 오름차순).
     */
    @Query("""
            SELECT v FROM MaskingPolicy v
            WHERE v.validTo IS NULL
              AND v.status = com.example.common.version.VersionStatus.PUBLISHED
              AND v.active = true
            ORDER BY v.priority ASC
            """)
    List<MaskingPolicy> findAllCurrentActiveVersions();
}
