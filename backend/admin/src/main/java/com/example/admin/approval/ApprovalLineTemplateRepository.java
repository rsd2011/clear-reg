package com.example.admin.approval;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalLineTemplateRepository extends JpaRepository<ApprovalLineTemplate, UUID> {

    Optional<ApprovalLineTemplate> findByIdAndActiveTrue(UUID id);
}
