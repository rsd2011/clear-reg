package com.example.admin.approval.repository;

import java.util.List;
import java.util.UUID;

import com.example.admin.approval.domain.ApprovalTemplateStepVersion;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 승인선 템플릿 단계 버전 Repository.
 */
public interface ApprovalTemplateStepVersionRepository extends JpaRepository<ApprovalTemplateStepVersion, UUID> {

    /**
     * 특정 템플릿 버전의 Step 목록 조회 (순서대로).
     */
    List<ApprovalTemplateStepVersion> findByTemplateVersionIdOrderByStepOrderAsc(UUID templateVersionId);

    /**
     * 특정 템플릿 버전의 Step 삭제.
     */
    void deleteByTemplateVersionId(UUID templateVersionId);
}
