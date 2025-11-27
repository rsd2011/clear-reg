package com.example.admin.approval;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalGroupRepository extends JpaRepository<ApprovalGroup, UUID> {

    Optional<ApprovalGroup> findByGroupCode(String groupCode);
}
