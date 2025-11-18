package com.example.auth.permission.context;

import com.example.auth.permission.PermissionAssignment;
import com.example.auth.permission.PermissionGroup;
import com.example.auth.domain.UserAccount;

public record PermissionDecision(UserAccount account,
                                 PermissionAssignment assignment,
                                 PermissionGroup group) {

    public AuthContext toContext() {
        return new AuthContext(account.getUsername(),
                account.getOrganizationCode(),
                account.getPermissionGroupCode(),
                assignment.getFeature(),
                assignment.getAction(),
                assignment.getRowScope(),
                group.maskRulesByTag());
    }
}
