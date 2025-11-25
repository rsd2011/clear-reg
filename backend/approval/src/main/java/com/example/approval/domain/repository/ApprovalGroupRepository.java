package com.example.approval.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.approval.domain.ApprovalGroup;

public interface ApprovalGroupRepository extends JpaRepository<ApprovalGroup, UUID> {

    Optional<ApprovalGroup> findByGroupCode(String groupCode);
}
