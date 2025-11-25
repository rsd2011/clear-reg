package com.example.auth.permission.check;

import com.example.auth.domain.UserAccount;
import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.PermissionAssignment;
import com.example.auth.permission.PermissionGroup;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;

@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Immutable context intentionally references domain objects")
public class PermissionEvaluationContext {

  private final FeatureCode feature;
  private final ActionCode action;
  private final UserAccount account;
  private final PermissionGroup group;
  private final PermissionAssignment assignment;
  private final Map<String, Object> attributes;

  public PermissionEvaluationContext(
      FeatureCode feature,
      ActionCode action,
      UserAccount account,
      PermissionGroup group,
      PermissionAssignment assignment,
      Map<String, Object> attributes) {
    this.feature = feature;
    this.action = action;
    this.account = account;
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

  public UserAccount account() {
    return account;
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
