package com.example.auth.permission;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountService;
import com.example.auth.organization.OrganizationPolicyService;
import com.example.auth.permission.context.PermissionDecision;

@Component
public class PermissionEvaluator {

    private final UserAccountService userAccountService;
    private final PermissionGroupService permissionGroupService;
    private final OrganizationPolicyService organizationPolicyService;

    public PermissionEvaluator(UserAccountService userAccountService,
                               PermissionGroupService permissionGroupService,
                               OrganizationPolicyService organizationPolicyService) {
        this.userAccountService = userAccountService;
        this.permissionGroupService = permissionGroupService;
        this.organizationPolicyService = organizationPolicyService;
    }

    public PermissionDecision evaluate(FeatureCode feature, ActionCode action) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new PermissionDeniedException("인증 정보가 없습니다.");
        }
        String username = authentication.getName();
        UserAccount account = userAccountService.getByUsernameOrThrow(username);
        String groupCode = determineGroupCode(account);
        PermissionGroup group = permissionGroupService.getByCodeOrThrow(groupCode);
        PermissionAssignment assignment = group.assignmentFor(feature, action)
                .orElseThrow(() -> new PermissionDeniedException("권한이 없습니다: " + feature + " " + action));
        return new PermissionDecision(account, assignment, group);
    }

    private String determineGroupCode(UserAccount account) {
        String groupCode = account.getPermissionGroupCode();
        if (groupCode == null || groupCode.isBlank()) {
            return organizationPolicyService.defaultPermissionGroup(account.getOrganizationCode());
        }
        return groupCode;
    }
}
