package com.example.draft.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.draft.domain.ApprovalGroup;

public interface ApprovalGroupRepository extends JpaRepository<ApprovalGroup, java.util.UUID> {

    Optional<ApprovalGroup> findByGroupCode(String groupCode);
}
