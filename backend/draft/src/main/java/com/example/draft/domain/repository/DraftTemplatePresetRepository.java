package com.example.draft.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.draft.domain.DraftTemplatePreset;

public interface DraftTemplatePresetRepository extends JpaRepository<DraftTemplatePreset, UUID> {

    Optional<DraftTemplatePreset> findByIdAndActiveTrue(UUID id);

    List<DraftTemplatePreset> findByBusinessFeatureCodeAndOrganizationCodeAndActiveTrue(String businessFeatureCode, String organizationCode);

    List<DraftTemplatePreset> findByBusinessFeatureCodeAndOrganizationCodeIsNullAndActiveTrue(String businessFeatureCode);

    Optional<DraftTemplatePreset> findByPresetCodeAndActiveTrue(String presetCode);
}
