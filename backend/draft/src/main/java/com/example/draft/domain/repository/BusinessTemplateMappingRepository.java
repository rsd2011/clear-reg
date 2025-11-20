package com.example.draft.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.draft.domain.BusinessTemplateMapping;

public interface BusinessTemplateMappingRepository extends JpaRepository<BusinessTemplateMapping, UUID> {

    Optional<BusinessTemplateMapping> findByBusinessFeatureCodeAndOrganizationCode(String businessFeatureCode, String organizationCode);

    Optional<BusinessTemplateMapping> findByBusinessFeatureCodeAndOrganizationCodeIsNull(String businessFeatureCode);
}
