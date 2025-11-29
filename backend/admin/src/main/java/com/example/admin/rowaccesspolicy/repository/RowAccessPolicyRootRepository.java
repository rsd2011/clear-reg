package com.example.admin.rowaccesspolicy.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.admin.permission.domain.FeatureCode;
import com.example.admin.rowaccesspolicy.domain.RowAccessPolicyRoot;
import com.example.common.security.RowScope;

public interface RowAccessPolicyRootRepository extends JpaRepository<RowAccessPolicyRoot, UUID> {

    /**
     * 정책 코드로 루트 조회.
     */
    Optional<RowAccessPolicyRoot> findByPolicyCode(String policyCode);

    /**
     * 정책 코드 존재 여부 확인.
     */
    boolean existsByPolicyCode(String policyCode);

    /**
     * ID로 조회하면서 현재 활성 버전이 active 상태인 루트만 반환합니다.
     */
    @Query("""
            SELECT r FROM RowAccessPolicyRoot r
            WHERE r.id = :id
              AND r.currentVersion IS NOT NULL
              AND r.currentVersion.active = true
            """)
    Optional<RowAccessPolicyRoot> findByIdAndActiveVersion(@Param("id") UUID id);

    /**
     * 현재 버전이 활성화된 모든 루트 조회 (우선순위순).
     */
    @Query("""
            SELECT r FROM RowAccessPolicyRoot r
            WHERE r.currentVersion IS NOT NULL
              AND r.currentVersion.active = true
            ORDER BY r.currentVersion.priority ASC
            """)
    List<RowAccessPolicyRoot> findAllActiveOrderByPriority();

    /**
     * 현재 버전이 활성화된 모든 루트 조회 (페이징).
     */
    @Query("""
            SELECT r FROM RowAccessPolicyRoot r
            LEFT JOIN FETCH r.currentVersion v
            WHERE v IS NOT NULL
              AND v.active = true
            ORDER BY v.priority ASC
            """)
    Page<RowAccessPolicyRoot> findAllWithActiveCurrentVersion(Pageable pageable);

    /**
     * 현재 버전이 있는 모든 루트 조회 (페이징).
     */
    @Query("""
            SELECT r FROM RowAccessPolicyRoot r
            LEFT JOIN FETCH r.currentVersion
            WHERE r.currentVersion IS NOT NULL
            ORDER BY r.updatedAt DESC
            """)
    Page<RowAccessPolicyRoot> findAllWithCurrentVersion(Pageable pageable);

    /**
     * 특정 FeatureCode로 필터링된 활성 루트 조회.
     */
    @Query("""
            SELECT r FROM RowAccessPolicyRoot r
            LEFT JOIN FETCH r.currentVersion v
            WHERE v IS NOT NULL
              AND v.active = true
              AND v.featureCode = :featureCode
            ORDER BY v.priority ASC
            """)
    List<RowAccessPolicyRoot> findActiveByFeatureCode(@Param("featureCode") FeatureCode featureCode);

    /**
     * 특정 FeatureCode로 필터링된 루트 조회 (페이징).
     */
    @Query("""
            SELECT r FROM RowAccessPolicyRoot r
            LEFT JOIN FETCH r.currentVersion v
            WHERE v IS NOT NULL
              AND v.featureCode = :featureCode
            ORDER BY v.priority ASC
            """)
    Page<RowAccessPolicyRoot> findByFeatureCode(
            @Param("featureCode") FeatureCode featureCode,
            Pageable pageable);

    /**
     * 특정 RowScope로 필터링된 활성 루트 조회.
     */
    @Query("""
            SELECT r FROM RowAccessPolicyRoot r
            LEFT JOIN FETCH r.currentVersion v
            WHERE v IS NOT NULL
              AND v.active = true
              AND v.rowScope = :rowScope
            ORDER BY v.priority ASC
            """)
    List<RowAccessPolicyRoot> findActiveByRowScope(@Param("rowScope") RowScope rowScope);

    /**
     * 특정 RowScope로 필터링된 루트 조회 (페이징).
     */
    @Query("""
            SELECT r FROM RowAccessPolicyRoot r
            LEFT JOIN FETCH r.currentVersion v
            WHERE v IS NOT NULL
              AND v.rowScope = :rowScope
            ORDER BY v.priority ASC
            """)
    Page<RowAccessPolicyRoot> findByRowScope(
            @Param("rowScope") RowScope rowScope,
            Pageable pageable);

    /**
     * 초안이 있는 루트 목록 조회.
     */
    @Query("""
            SELECT r FROM RowAccessPolicyRoot r
            WHERE r.nextVersion IS NOT NULL
            ORDER BY r.updatedAt DESC
            """)
    List<RowAccessPolicyRoot> findAllWithDraft();
}
