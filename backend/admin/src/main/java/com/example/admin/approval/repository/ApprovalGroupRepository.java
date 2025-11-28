package com.example.admin.approval.repository;

import java.util.Optional;
import java.util.UUID;

import com.example.admin.approval.domain.ApprovalGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalGroupRepository extends JpaRepository<ApprovalGroup, UUID> {

    Optional<ApprovalGroup> findByGroupCode(String groupCode);

    boolean existsByGroupCode(String groupCode);
}
