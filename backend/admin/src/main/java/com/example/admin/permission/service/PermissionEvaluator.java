package com.example.admin.permission.service;

import com.example.admin.permission.context.PermissionDecision;
import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.admin.permission.domain.PermissionAssignment;
import com.example.admin.permission.domain.PermissionGroup;
import com.example.admin.permission.exception.PermissionDeniedException;
import com.example.admin.permission.spi.UserInfo;
import com.example.admin.permission.spi.UserInfoProvider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Evaluator returns domain objects by design")
@Component
public class PermissionEvaluator {

  private static final String DEFAULT_PERMISSION_GROUP = "DEFAULT";

  private final UserInfoProvider userInfoProvider;
  private final PermissionGroupService permissionGroupService;

  public PermissionEvaluator(
      UserInfoProvider userInfoProvider,
      PermissionGroupService permissionGroupService) {
    this.userInfoProvider = userInfoProvider;
    this.permissionGroupService = permissionGroupService;
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
    return new PermissionDecision(userInfo, assignment, group);
  }

  private String determineGroupCode(UserInfo userInfo) {
    String groupCode = userInfo.getPermissionGroupCode();
    if (groupCode == null || groupCode.isBlank()) {
      return DEFAULT_PERMISSION_GROUP;
    }
    return groupCode;
  }
}
