package com.example.admin.approval.repository;

import java.util.Optional;
import java.util.UUID;

import com.example.admin.approval.domain.ApprovalLineTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalLineTemplateRepository extends JpaRepository<ApprovalLineTemplate, UUID> {

    Optional<ApprovalLineTemplate> findByIdAndActiveTrue(UUID id);
}
