package com.example.server.user.dto;

import com.example.admin.user.domain.UserAccount;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * 사용자 상세 응답 DTO.
 */
public record UserDetailResponse(
    UUID id,
    String username,
    String email,
    String organizationCode,
    String permissionGroupCode,
    String employeeId,
    String ssoId,
    String activeDirectoryDomain,
    Set<String> roles,
    boolean active,
    int failedLoginAttempts,
    Instant lockedUntil,
    boolean locked,
    Instant passwordChangedAt,
    Instant lastLoginAt
) {

  public static UserDetailResponse from(UserAccount account) {
    return new UserDetailResponse(
        account.getId(),
        account.getUsername(),
        account.getEmail(),
        account.getOrganizationCode(),
        account.getPermissionGroupCode(),
        account.getEmployeeId(),
        account.getSsoId(),
        account.getActiveDirectoryDomain(),
        account.getRoles(),
        account.isActive(),
        account.getFailedLoginAttempts(),
        account.getLockedUntil(),
        account.isLocked(),
        account.getPasswordChangedAt(),
        account.getLastLoginAt()
    );
  }
}
