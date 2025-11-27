package com.example.draft.domain.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.draft.domain.Draft;

public interface DraftRepository extends JpaRepository<Draft, UUID>, JpaSpecificationExecutor<Draft> {

    java.util.List<Draft> findTop5ByCreatedByAndBusinessFeatureCodeOrderByCreatedAtDesc(String createdBy, String businessFeatureCode);
}
