package com.example.admin.maskingpolicy.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.admin.maskingpolicy.domain.MaskingPolicyRoot;
import com.example.common.security.FeatureCode;

/**
 * 마스킹 정책 루트 Repository.
 */
public interface MaskingPolicyRootRepository extends JpaRepository<MaskingPolicyRoot, UUID> {

    /**
     * 정책 코드로 조회.
     */
    Optional<MaskingPolicyRoot> findByPolicyCode(String policyCode);

    /**
     * 정책 코드 존재 여부 확인.
     */
    boolean existsByPolicyCode(String policyCode);

    /**
     * ID로 조회하면서 현재 활성 버전이 active 상태인 루트만 반환합니다.
     */
    @Query("""
            SELECT r FROM MaskingPolicyRoot r
            WHERE r.id = :id
              AND r.currentVersion IS NOT NULL
              AND r.currentVersion.active = true
            """)
    Optional<MaskingPolicyRoot> findByIdAndActiveVersion(@Param("id") UUID id);

    /**
     * 현재 활성 버전이 있는 모든 루트 조회 (우선순위 오름차순).
     */
    @Query("""
            SELECT r FROM MaskingPolicyRoot r
            WHERE r.currentVersion IS NOT NULL
              AND r.currentVersion.active = true
            ORDER BY r.currentVersion.priority ASC
            """)
    List<MaskingPolicyRoot> findAllActiveOrderByPriority();

    /**
     * 페이지네이션 지원 목록 조회.
     */
    @Query("""
            SELECT r FROM MaskingPolicyRoot r
            WHERE r.currentVersion IS NOT NULL
            ORDER BY r.currentVersion.priority ASC
            """)
    Page<MaskingPolicyRoot> findAllWithCurrentVersion(Pageable pageable);

    /**
     * 특정 FeatureCode를 가진 활성 정책 루트 조회.
     */
    @Query("""
            SELECT r FROM MaskingPolicyRoot r
            WHERE r.currentVersion IS NOT NULL
              AND r.currentVersion.active = true
              AND r.currentVersion.featureCode = :featureCode
            ORDER BY r.currentVersion.priority ASC
            """)
    List<MaskingPolicyRoot> findActiveByFeatureCode(@Param("featureCode") FeatureCode featureCode);

    /**
     * 초안이 있는 루트 목록 조회.
     */
    @Query("""
            SELECT r FROM MaskingPolicyRoot r
            WHERE r.nextVersion IS NOT NULL
            ORDER BY r.updatedAt DESC
            """)
    List<MaskingPolicyRoot> findAllWithDraft();
}
