package com.example.admin.permission.service;

import com.example.admin.permission.check.PermissionCheck;
import com.example.admin.permission.check.PermissionEvaluationContext;
import com.example.admin.permission.context.PermissionDecision;
import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.admin.permission.domain.PermissionAssignment;
import com.example.admin.permission.domain.PermissionGroup;
import com.example.admin.permission.exception.PermissionDeniedException;
import com.example.admin.permission.spi.OrganizationPolicyProvider;
import com.example.admin.permission.spi.UserInfo;
import com.example.admin.permission.spi.UserInfoProvider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Evaluator returns domain objects by design; context is immutable snapshot")
@Component
public class PermissionEvaluator {

  private final UserInfoProvider userInfoProvider;
  private final PermissionGroupService permissionGroupService;
  private final OrganizationPolicyProvider organizationPolicyProvider;
  private final List<PermissionCheck> permissionChecks;

  public PermissionEvaluator(
      UserInfoProvider userInfoProvider,
      PermissionGroupService permissionGroupService,
      OrganizationPolicyProvider organizationPolicyProvider,
      List<PermissionCheck> permissionChecks) {
    this.userInfoProvider = userInfoProvider;
    this.permissionGroupService = permissionGroupService;
    this.organizationPolicyProvider = organizationPolicyProvider;
    this.permissionChecks = permissionChecks == null ? List.of() : permissionChecks;
  }

  public PermissionDecision evaluate(FeatureCode feature, ActionCode action) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new PermissionDeniedException("인증 정보가 없습니다.");
    }
    String username = authentication.getName();
    UserInfo userInfo = userInfoProvider.getByUsernameOrThrow(username);
    String groupCode = determineGroupCode(userInfo);
    PermissionGroup group = permissionGroupService.getByCodeOrThrow(groupCode);
    PermissionAssignment assignment =
        group
            .assignmentFor(feature, action)
            .orElseThrow(
                () -> new PermissionDeniedException("권한이 없습니다: " + feature + " " + action));
    PermissionEvaluationContext context =
        new PermissionEvaluationContext(
            feature,
            action,
            userInfo,
            group,
            assignment,
            buildAttributes(userInfo, group, feature, action));
    for (PermissionCheck check : permissionChecks) {
      check.check(context);
    }
    return new PermissionDecision(userInfo, assignment, group);
  }

  private String determineGroupCode(UserInfo userInfo) {
    String groupCode = userInfo.getPermissionGroupCode();
    if (groupCode == null || groupCode.isBlank()) {
      return organizationPolicyProvider.defaultPermissionGroup(userInfo.getOrganizationCode());
    }
    return groupCode;
  }

  private Map<String, Object> buildAttributes(
      UserInfo userInfo, PermissionGroup group, FeatureCode feature, ActionCode action) {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("username", userInfo.getUsername());
    attributes.put("organizationCode", userInfo.getOrganizationCode());
    attributes.put("permissionGroupCode", group.getCode());
    attributes.put("permissionGroupName", group.getName());
    attributes.put("defaultRowScope", group.getDefaultRowScope().name());
    attributes.put("roles", userInfo.getRoles());
    attributes.put("feature", feature.name());
    attributes.put("action", action.name());
    return attributes;
  }
}
