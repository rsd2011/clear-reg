package com.example.admin.approval.repository;

import java.util.List;
import java.util.UUID;

import com.example.admin.approval.domain.ApprovalTemplateStep;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 승인선 템플릿 단계 Repository.
 */
public interface ApprovalTemplateStepRepository extends JpaRepository<ApprovalTemplateStep, UUID> {

    /**
     * 특정 템플릿(버전)의 Step 목록 조회 (순서대로).
     *
     * @param templateId ApprovalTemplate(버전)의 ID
     */
    List<ApprovalTemplateStep> findByTemplateIdOrderByStepOrderAsc(UUID templateId);

    /**
     * 특정 템플릿(버전)의 Step 삭제.
     *
     * @param templateId ApprovalTemplate(버전)의 ID
     */
    void deleteByTemplateId(UUID templateId);
}
