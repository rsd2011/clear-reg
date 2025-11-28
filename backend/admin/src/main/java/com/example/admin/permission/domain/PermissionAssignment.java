package com.example.admin.permission.domain;

import com.example.common.security.RowScope;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.Optional;

@Embeddable
public class PermissionAssignment {

  @Enumerated(EnumType.STRING)
  @Column(name = "feature_code", nullable = false)
  private FeatureCode feature;

  @Enumerated(EnumType.STRING)
  @Column(name = "action_code", nullable = false)
  private ActionCode action;

  @Enumerated(EnumType.STRING)
  @Column(name = "row_scope", nullable = false)
  private RowScope rowScope = RowScope.OWN;

  @Column(name = "row_condition_expression")
  private String rowConditionExpression;

  protected PermissionAssignment() {}

  public PermissionAssignment(FeatureCode feature, ActionCode action, RowScope rowScope) {
    this(feature, action, rowScope, null);
  }

  public PermissionAssignment(
      FeatureCode feature, ActionCode action, RowScope rowScope, String rowConditionExpression) {
    this.feature = feature;
    this.action = action;
    this.rowScope = rowScope == null ? RowScope.OWN : rowScope;
    this.rowConditionExpression =
        (rowConditionExpression == null || rowConditionExpression.isBlank())
            ? null
            : rowConditionExpression;
  }

  public FeatureCode getFeature() {
    return feature;
  }

  public ActionCode getAction() {
    return action;
  }

  public RowScope getRowScope() {
    return rowScope;
  }

  public Optional<String> getRowConditionExpression() {
    return Optional.ofNullable(rowConditionExpression);
  }
}
