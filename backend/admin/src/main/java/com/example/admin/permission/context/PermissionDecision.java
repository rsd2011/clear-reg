package com.example.admin.permission.context;

import com.example.admin.permission.PermissionAssignment;
import com.example.admin.permission.PermissionGroup;
import com.example.admin.permission.spi.UserInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * 권한 평가 결과를 담는 레코드.
 *
 * <p>DataPolicy 기반 마스킹으로 마이그레이션되어 maskRules 전달 제거됨.
 */
@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Value object forwards domain references deliberately")
public record PermissionDecision(
    UserInfo userInfo, PermissionAssignment assignment, PermissionGroup group) {

  public AuthContext toContext() {
    return AuthContext.of(
        userInfo.getUsername(),
        userInfo.getOrganizationCode(),
        userInfo.getPermissionGroupCode(),
        assignment.getFeature(),
        assignment.getAction(),
        assignment.getRowScope());
  }
}
