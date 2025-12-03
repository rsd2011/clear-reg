package com.example.admin.permission.context;

import com.example.admin.permission.domain.PermissionAssignment;
import com.example.admin.permission.domain.PermissionGroup;
import com.example.common.user.spi.UserAccountInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Objects;

/**
 * 권한 평가 결과를 담는 도메인 객체.
 *
 * <p>RowScope는 RowAccessPolicy로 이관되어 AuthContext에서 제거됨.
 *
 * <p>도메인 로직({@link #toContext()})을 포함하므로 Class로 구현됨.
 */
@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Value object forwards domain references deliberately")
public final class PermissionDecision {

  private final UserAccountInfo userInfo;
  private final PermissionAssignment assignment;
  private final PermissionGroup group;

  public PermissionDecision(
      UserAccountInfo userInfo, PermissionAssignment assignment, PermissionGroup group) {
    this.userInfo = userInfo;
    this.assignment = assignment;
    this.group = group;
  }

  public UserAccountInfo userInfo() {
    return userInfo;
  }

  public PermissionAssignment assignment() {
    return assignment;
  }

  public PermissionGroup group() {
    return group;
  }

  /**
   * 권한 결정을 기반으로 인증 컨텍스트를 생성한다.
   *
   * @return AuthContext 인증 컨텍스트
   */
  public AuthContext toContext() {
    return AuthContext.of(
        userInfo.getUsername(),
        userInfo.getOrganizationCode(),
        userInfo.getPermissionGroupCode(),
        assignment.getFeature(),
        assignment.getAction());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PermissionDecision that)) return false;
    return Objects.equals(userInfo, that.userInfo)
        && Objects.equals(assignment, that.assignment)
        && Objects.equals(group, that.group);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userInfo, assignment, group);
  }

  @Override
  public String toString() {
    return "PermissionDecision["
        + "userInfo=" + userInfo
        + ", assignment=" + assignment
        + ", group=" + group
        + ']';
  }
}
