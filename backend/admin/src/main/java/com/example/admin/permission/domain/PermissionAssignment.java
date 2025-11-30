package com.example.admin.permission.domain;

import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.Objects;

/**
 * 권한 할당 VO.
 * 
 * <p>특정 기능(Feature)에 대한 액션(Action) 권한을 나타냅니다.</p>
 */
@Embeddable
public class PermissionAssignment {

  @Enumerated(EnumType.STRING)
  @Column(name = "feature_code", nullable = false)
  private FeatureCode feature;

  @Enumerated(EnumType.STRING)
  @Column(name = "action_code", nullable = false)
  private ActionCode action;

  protected PermissionAssignment() {}

  public PermissionAssignment(FeatureCode feature, ActionCode action) {
    this.feature = feature;
    this.action = action;
  }

  public FeatureCode getFeature() {
    return feature;
  }

  public ActionCode getAction() {
    return action;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PermissionAssignment that = (PermissionAssignment) o;
    return feature == that.feature && action == that.action;
  }

  @Override
  public int hashCode() {
    return Objects.hash(feature, action);
  }

  @Override
  public String toString() {
    return "PermissionAssignment{" +
           "feature=" + feature +
           ", action=" + action +
           '}';
  }
}
