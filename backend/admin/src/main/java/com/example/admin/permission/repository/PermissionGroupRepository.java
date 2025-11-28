package com.example.admin.permission.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.admin.permission.domain.PermissionGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionGroupRepository extends JpaRepository<PermissionGroup, UUID> {

  Optional<PermissionGroup> findByCode(String code);

  List<PermissionGroup> findByApprovalGroupCode(String approvalGroupCode);
}
