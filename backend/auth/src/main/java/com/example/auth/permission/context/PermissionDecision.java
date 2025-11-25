package com.example.auth.permission.context;

import com.example.auth.domain.UserAccount;
import com.example.auth.permission.PermissionAssignment;
import com.example.auth.permission.PermissionGroup;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Value object forwards domain references deliberately")
public record PermissionDecision(
    UserAccount account, PermissionAssignment assignment, PermissionGroup group) {

  public AuthContext toContext() {
    return new AuthContext(
        account.getUsername(),
        account.getOrganizationCode(),
        account.getPermissionGroupCode(),
        assignment.getFeature(),
        assignment.getAction(),
        assignment.getRowScope(),
        group.maskRulesByTag());
  }
}
