package com.example.auth.permission;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountService;
import com.example.auth.organization.OrganizationPolicyService;
import com.example.auth.permission.check.PermissionCheck;
import com.example.auth.permission.check.PermissionEvaluationContext;
import com.example.auth.permission.context.PermissionDecision;

@Component
public class PermissionEvaluator {

    private final UserAccountService userAccountService;
    private final PermissionGroupService permissionGroupService;
    private final OrganizationPolicyService organizationPolicyService;
    private final List<PermissionCheck> permissionChecks;

    public PermissionEvaluator(UserAccountService userAccountService,
                               PermissionGroupService permissionGroupService,
                               OrganizationPolicyService organizationPolicyService,
                               List<PermissionCheck> permissionChecks) {
        this.userAccountService = userAccountService;
        this.permissionGroupService = permissionGroupService;
        this.organizationPolicyService = organizationPolicyService;
        this.permissionChecks = permissionChecks == null ? List.of() : permissionChecks;
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
        PermissionEvaluationContext context = new PermissionEvaluationContext(feature, action, account, group,
                assignment, buildAttributes(account, group, feature, action));
        for (PermissionCheck check : permissionChecks) {
            check.check(context);
        }
        return new PermissionDecision(account, assignment, group);
    }

    private String determineGroupCode(UserAccount account) {
        String groupCode = account.getPermissionGroupCode();
        if (groupCode == null || groupCode.isBlank()) {
            return organizationPolicyService.defaultPermissionGroup(account.getOrganizationCode());
        }
        return groupCode;
    }

    private Map<String, Object> buildAttributes(UserAccount account,
                                                PermissionGroup group,
                                                FeatureCode feature,
                                                ActionCode action) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("username", account.getUsername());
        attributes.put("organizationCode", account.getOrganizationCode());
        attributes.put("permissionGroupCode", group.getCode());
        attributes.put("permissionGroupName", group.getName());
        attributes.put("defaultRowScope", group.getDefaultRowScope().name());
        attributes.put("roles", account.getRoles());
        attributes.put("feature", feature.name());
        attributes.put("action", action.name());
        return attributes;
    }
}
