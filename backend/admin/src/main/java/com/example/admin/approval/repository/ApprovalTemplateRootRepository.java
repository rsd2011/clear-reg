package com.example.admin.approval.repository;

import java.util.Optional;
import java.util.UUID;

import com.example.admin.approval.domain.ApprovalTemplateRoot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApprovalTemplateRootRepository extends JpaRepository<ApprovalTemplateRoot, UUID> {

    /**
     * ID로 조회하면서 현재 활성 버전이 active 상태인 루트만 반환합니다.
     */
    @Query("""
            SELECT r FROM ApprovalTemplateRoot r
            WHERE r.id = :id
              AND r.currentVersion IS NOT NULL
              AND r.currentVersion.active = true
            """)
    Optional<ApprovalTemplateRoot> findByIdAndActiveVersion(@Param("id") UUID id);
}
