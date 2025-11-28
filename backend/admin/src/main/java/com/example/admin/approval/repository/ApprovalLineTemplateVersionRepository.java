package com.example.admin.approval.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.admin.approval.domain.ApprovalLineTemplateVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 승인선 템플릿 버전 Repository.
 */
public interface ApprovalLineTemplateVersionRepository extends JpaRepository<ApprovalLineTemplateVersion, UUID> {

    /**
     * 현재 유효한 게시 버전 조회 (valid_to IS NULL AND status = PUBLISHED).
     */
    @Query("""
            SELECT v FROM ApprovalLineTemplateVersion v
            WHERE v.template.id = :templateId
              AND v.validTo IS NULL
              AND v.status = com.example.common.version.VersionStatus.PUBLISHED
            """)
    Optional<ApprovalLineTemplateVersion> findCurrentByTemplateId(@Param("templateId") UUID templateId);

    /**
     * 특정 시점의 버전 조회 (시점 조회).
     */
    @Query("""
            SELECT v FROM ApprovalLineTemplateVersion v
            WHERE v.template.id = :templateId
              AND v.validFrom <= :asOf
              AND (v.validTo IS NULL OR v.validTo > :asOf)
              AND v.status != com.example.common.version.VersionStatus.DRAFT
            """)
    Optional<ApprovalLineTemplateVersion> findByTemplateIdAsOf(
            @Param("templateId") UUID templateId,
            @Param("asOf") OffsetDateTime asOf);

    /**
     * 버전 이력 조회 (최신순, Draft 제외).
     */
    @Query("""
            SELECT v FROM ApprovalLineTemplateVersion v
            WHERE v.template.id = :templateId
              AND v.status != com.example.common.version.VersionStatus.DRAFT
            ORDER BY v.version DESC
            """)
    List<ApprovalLineTemplateVersion> findHistoryByTemplateId(@Param("templateId") UUID templateId);

    /**
     * 버전 이력 조회 (최신순, Draft 포함).
     */
    List<ApprovalLineTemplateVersion> findByTemplateIdOrderByVersionDesc(UUID templateId);

    /**
     * 특정 버전 번호로 조회.
     */
    Optional<ApprovalLineTemplateVersion> findByTemplateIdAndVersion(UUID templateId, Integer version);

    /**
     * 최신 버전 번호 조회.
     */
    @Query("SELECT COALESCE(MAX(v.version), 0) FROM ApprovalLineTemplateVersion v WHERE v.template.id = :templateId")
    int findMaxVersionByTemplateId(@Param("templateId") UUID templateId);

    /**
     * 이력 개수 조회 (Draft 제외).
     */
    @Query("""
            SELECT COUNT(v) FROM ApprovalLineTemplateVersion v
            WHERE v.template.id = :templateId
              AND v.status != com.example.common.version.VersionStatus.DRAFT
            """)
    long countHistoryByTemplateId(@Param("templateId") UUID templateId);

    /**
     * 초안 버전 조회 (템플릿당 최대 1개).
     */
    @Query("""
            SELECT v FROM ApprovalLineTemplateVersion v
            WHERE v.template.id = :templateId
              AND v.status = com.example.common.version.VersionStatus.DRAFT
            """)
    Optional<ApprovalLineTemplateVersion> findDraftByTemplateId(@Param("templateId") UUID templateId);

    /**
     * 초안이 있는지 확인.
     */
    @Query("""
            SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END
            FROM ApprovalLineTemplateVersion v
            WHERE v.template.id = :templateId
              AND v.status = com.example.common.version.VersionStatus.DRAFT
            """)
    boolean existsDraftByTemplateId(@Param("templateId") UUID templateId);

    /**
     * 두 버전 비교를 위한 조회.
     */
    @Query("""
            SELECT v FROM ApprovalLineTemplateVersion v
            WHERE v.template.id = :templateId
              AND v.version IN (:version1, :version2)
            ORDER BY v.version ASC
            """)
    List<ApprovalLineTemplateVersion> findVersionsForComparison(
            @Param("templateId") UUID templateId,
            @Param("version1") Integer version1,
            @Param("version2") Integer version2);
}
