package com.example.admin.permission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionGroupRepository extends JpaRepository<PermissionGroup, UUID> {

  Optional<PermissionGroup> findByCode(String code);

  List<PermissionGroup> findByApprovalGroupCode(String approvalGroupCode);
}
