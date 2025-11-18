package com.example.policy;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyDocumentRepository extends JpaRepository<PolicyDocument, UUID> {

    Optional<PolicyDocument> findByCode(String code);
}
