package com.example.approval.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.approval.domain.ApprovalLineTemplate;

public interface ApprovalLineTemplateRepository extends JpaRepository<ApprovalLineTemplate, UUID> {

    Optional<ApprovalLineTemplate> findByIdAndActiveTrue(UUID id);
}
