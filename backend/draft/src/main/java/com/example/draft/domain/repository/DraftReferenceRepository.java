package com.example.draft.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.draft.domain.DraftReference;

public interface DraftReferenceRepository extends JpaRepository<DraftReference, UUID> {

    List<DraftReference> findByDraftIdAndActiveTrue(UUID draftId);
}
