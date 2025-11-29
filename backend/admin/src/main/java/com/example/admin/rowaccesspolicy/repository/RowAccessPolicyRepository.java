package com.example.admin.rowaccesspolicy.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.admin.rowaccesspolicy.domain.RowAccessPolicy;
import com.example.admin.rowaccesspolicy.domain.RowAccessPolicyRoot;
import com.example.common.version.VersionStatus;

public interface RowAccessPolicyRepository extends JpaRepository<RowAccessPolicy, UUID> {

    /**
     * 활성화된 현재 버전들 조회 (정책 평가용).
     * validTo가 null이고 status가 PUBLISHED이며 active가 true인 버전들.
     */
    @Query("""
            SELECT p FROM RowAccessPolicy p
            WHERE p.validTo IS NULL
              AND p.status = 'PUBLISHED'
              AND p.active = true
            ORDER BY p.priority ASC
            """)
    List<RowAccessPolicy> findAllCurrentActiveVersions();

    /**
     * 특정 루트의 모든 버전 조회 (이력 조회용).
     */
    @Query("""
            SELECT p FROM RowAccessPolicy p
            WHERE p.root = :root
            ORDER BY p.version DESC
            """)
    Page<RowAccessPolicy> findByRootOrderByVersionDesc(
            @Param("root") RowAccessPolicyRoot root,
            Pageable pageable);

    /**
     * 특정 루트의 모든 버전 조회 (리스트).
     */
    List<RowAccessPolicy> findByRootOrderByVersionDesc(RowAccessPolicyRoot root);

    /**
     * 특정 루트의 특정 버전 조회.
     */
    Optional<RowAccessPolicy> findByRootAndVersion(RowAccessPolicyRoot root, Integer version);

    /**
     * 특정 루트의 최신 버전 번호 조회.
     */
    @Query("""
            SELECT MAX(p.version) FROM RowAccessPolicy p
            WHERE p.root = :root
            """)
    Optional<Integer> findMaxVersionByRoot(@Param("root") RowAccessPolicyRoot root);

    /**
     * 특정 루트의 현재 활성 버전 조회.
     */
    @Query("""
            SELECT p FROM RowAccessPolicy p
            WHERE p.root = :root
              AND p.validTo IS NULL
              AND p.status = 'PUBLISHED'
            """)
    Optional<RowAccessPolicy> findCurrentVersionByRoot(@Param("root") RowAccessPolicyRoot root);

    /**
     * 특정 루트의 초안 버전 조회.
     */
    @Query("""
            SELECT p FROM RowAccessPolicy p
            WHERE p.root = :root
              AND p.status = 'DRAFT'
            """)
    Optional<RowAccessPolicy> findDraftByRoot(@Param("root") RowAccessPolicyRoot root);

    /**
     * 특정 상태의 버전들 조회.
     */
    List<RowAccessPolicy> findByStatus(VersionStatus status);

    /**
     * 특정 루트의 버전 개수.
     */
    long countByRoot(RowAccessPolicyRoot root);

    // === rootId 기반 쿼리 메서드 (VersioningService용) ===

    /**
     * 현재 유효한 게시 버전 조회 (valid_to IS NULL AND status = PUBLISHED).
     */
    @Query("""
            SELECT v FROM RowAccessPolicy v
            WHERE v.root.id = :rootId
              AND v.validTo IS NULL
              AND v.status = com.example.common.version.VersionStatus.PUBLISHED
            """)
    Optional<RowAccessPolicy> findCurrentByRootId(@Param("rootId") UUID rootId);

    /**
     * 특정 시점의 버전 조회 (시점 조회).
     */
    @Query("""
            SELECT v FROM RowAccessPolicy v
            WHERE v.root.id = :rootId
              AND v.validFrom <= :asOf
              AND (v.validTo IS NULL OR v.validTo > :asOf)
              AND v.status != com.example.common.version.VersionStatus.DRAFT
            """)
    Optional<RowAccessPolicy> findByRootIdAsOf(
            @Param("rootId") UUID rootId,
            @Param("asOf") OffsetDateTime asOf);

    /**
     * 버전 이력 조회 (최신순, Draft 제외).
     */
    @Query("""
            SELECT v FROM RowAccessPolicy v
            WHERE v.root.id = :rootId
              AND v.status != com.example.common.version.VersionStatus.DRAFT
            ORDER BY v.version DESC
            """)
    List<RowAccessPolicy> findHistoryByRootId(@Param("rootId") UUID rootId);

    /**
     * 버전 이력 조회 (최신순, Draft 포함).
     */
    List<RowAccessPolicy> findByRootIdOrderByVersionDesc(UUID rootId);

    /**
     * 특정 버전 번호로 조회.
     */
    Optional<RowAccessPolicy> findByRootIdAndVersion(UUID rootId, Integer version);

    /**
     * 최신 버전 번호 조회.
     */
    @Query("SELECT COALESCE(MAX(v.version), 0) FROM RowAccessPolicy v WHERE v.root.id = :rootId")
    int findMaxVersionByRootId(@Param("rootId") UUID rootId);

    /**
     * 이력 개수 조회 (Draft 제외).
     */
    @Query("""
            SELECT COUNT(v) FROM RowAccessPolicy v
            WHERE v.root.id = :rootId
              AND v.status != com.example.common.version.VersionStatus.DRAFT
            """)
    long countHistoryByRootId(@Param("rootId") UUID rootId);

    /**
     * 초안 버전 조회 (정책당 최대 1개).
     */
    @Query("""
            SELECT v FROM RowAccessPolicy v
            WHERE v.root.id = :rootId
              AND v.status = com.example.common.version.VersionStatus.DRAFT
            """)
    Optional<RowAccessPolicy> findDraftByRootId(@Param("rootId") UUID rootId);

    /**
     * 초안이 있는지 확인.
     */
    @Query("""
            SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END
            FROM RowAccessPolicy v
            WHERE v.root.id = :rootId
              AND v.status = com.example.common.version.VersionStatus.DRAFT
            """)
    boolean existsDraftByRootId(@Param("rootId") UUID rootId);

    /**
     * 두 버전 비교를 위한 조회.
     */
    @Query("""
            SELECT v FROM RowAccessPolicy v
            WHERE v.root.id = :rootId
              AND v.version IN (:version1, :version2)
            ORDER BY v.version ASC
            """)
    List<RowAccessPolicy> findVersionsForComparison(
            @Param("rootId") UUID rootId,
            @Param("version1") Integer version1,
            @Param("version2") Integer version2);

}
