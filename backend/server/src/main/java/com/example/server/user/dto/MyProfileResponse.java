package com.example.server.user.dto;

import com.example.admin.user.domain.UserAccount;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * 내 정보 응답 DTO.
 */
public record MyProfileResponse(
    UUID id,
    String username,
    String email,
    String organizationCode,
    String permissionGroupCode,
    String employeeId,
    Set<String> roles,
    Instant passwordChangedAt,
    Instant lastLoginAt
) {

  public static MyProfileResponse from(UserAccount account) {
    return new MyProfileResponse(
        account.getId(),
        account.getUsername(),
        account.getEmail(),
        account.getOrganizationCode(),
        account.getPermissionGroupCode(),
        account.getEmployeeId(),
        account.getRoles(),
        account.getPasswordChangedAt(),
        account.getLastLoginAt()
    );
  }
}
