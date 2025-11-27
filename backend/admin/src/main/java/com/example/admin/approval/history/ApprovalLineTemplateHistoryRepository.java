package com.example.admin.approval.history;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 승인선 템플릿 변경 이력 Repository.
 */
public interface ApprovalLineTemplateHistoryRepository extends JpaRepository<ApprovalLineTemplateHistory, UUID> {

    /**
     * 특정 템플릿의 변경 이력을 최신순으로 조회.
     */
    @Query("SELECT h FROM ApprovalLineTemplateHistory h WHERE h.templateId = :templateId ORDER BY h.changedAt DESC")
    List<ApprovalLineTemplateHistory> findByTemplateIdOrderByChangedAtDesc(@Param("templateId") UUID templateId);

    /**
     * 특정 템플릿의 전체 이력 개수 조회.
     */
    long countByTemplateId(UUID templateId);
}
