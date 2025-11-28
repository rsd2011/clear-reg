package com.example.admin.policy.repository;

import java.util.Optional;
import java.util.UUID;

import com.example.admin.policy.domain.PolicyDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyDocumentRepository extends JpaRepository<PolicyDocument, UUID> {

    Optional<PolicyDocument> findByCode(String code);
}
