package com.example.server.user.dto;

import com.example.admin.user.domain.UserAccount;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * 사용자 목록 응답 DTO.
 */
public record UserListResponse(
    UUID id,
    String username,
    String email,
    String organizationCode,
    String permissionGroupCode,
    String employeeId,
    Set<String> roles,
    boolean active,
    boolean locked,
    Instant lastLoginAt
) {

  public static UserListResponse from(UserAccount account) {
    return new UserListResponse(
        account.getId(),
        account.getUsername(),
        account.getEmail(),
        account.getOrganizationCode(),
        account.getPermissionGroupCode(),
        account.getEmployeeId(),
        account.getRoles(),
        account.isActive(),
        account.isLocked(),
        account.getLastLoginAt()
    );
  }
}
