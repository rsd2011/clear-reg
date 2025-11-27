package com.example.admin.permission.check;

import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.PermissionAssignment;
import com.example.admin.permission.PermissionGroup;
import com.example.admin.permission.spi.UserInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;

@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Immutable context intentionally references domain objects")
public class PermissionEvaluationContext {

  private final FeatureCode feature;
  private final ActionCode action;
  private final UserInfo userInfo;
  private final PermissionGroup group;
  private final PermissionAssignment assignment;
  private final Map<String, Object> attributes;

  public PermissionEvaluationContext(
      FeatureCode feature,
      ActionCode action,
      UserInfo userInfo,
      PermissionGroup group,
      PermissionAssignment assignment,
      Map<String, Object> attributes) {
    this.feature = feature;
    this.action = action;
    this.userInfo = userInfo;
    this.group = group;
    this.assignment = assignment;
    this.attributes = Map.copyOf(attributes);
  }

  public FeatureCode feature() {
    return feature;
  }

  public ActionCode action() {
    return action;
  }

  public UserInfo userInfo() {
    return userInfo;
  }

  public PermissionGroup group() {
    return group;
  }

  public PermissionAssignment assignment() {
    return assignment;
  }

  public Map<String, Object> attributes() {
    return attributes;
  }
}
