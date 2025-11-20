package com.example.draft.domain.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.draft.domain.Draft;
import com.example.common.security.RequiresRowScope;

@RequiresRowScope
public interface DraftRepository extends JpaRepository<Draft, UUID>, JpaSpecificationExecutor<Draft> {
}
