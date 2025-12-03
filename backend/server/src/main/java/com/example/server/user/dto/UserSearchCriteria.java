package com.example.server.user.dto;

/**
 * 사용자 검색 조건 DTO.
 */
public record UserSearchCriteria(
    String username,
    String email,
    String organizationCode,
    String permissionGroupCode,
    Boolean active
) {

  public static UserSearchCriteria empty() {
    return new UserSearchCriteria(null, null, null, null, null);
  }
}
