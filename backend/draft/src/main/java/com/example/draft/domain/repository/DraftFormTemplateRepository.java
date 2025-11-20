package com.example.draft.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.draft.domain.DraftFormTemplate;

public interface DraftFormTemplateRepository extends JpaRepository<DraftFormTemplate, UUID> {

    Optional<DraftFormTemplate> findByIdAndActiveTrue(UUID id);
}
