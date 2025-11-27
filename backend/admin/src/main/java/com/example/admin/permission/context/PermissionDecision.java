package com.example.admin.permission.context;

import com.example.admin.permission.PermissionAssignment;
import com.example.admin.permission.PermissionGroup;
import com.example.admin.permission.spi.UserInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Value object forwards domain references deliberately")
public record PermissionDecision(
    UserInfo userInfo, PermissionAssignment assignment, PermissionGroup group) {

  public AuthContext toContext() {
    return new AuthContext(
        userInfo.getUsername(),
        userInfo.getOrganizationCode(),
        userInfo.getPermissionGroupCode(),
        assignment.getFeature(),
        assignment.getAction(),
        assignment.getRowScope(),
        group.maskRulesByTag());
  }
}
