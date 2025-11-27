package com.example.admin.permission;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.common.security.RowScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PermissionAssignmentTest {

  @Test
  @DisplayName("rowScope가 null이면 기본값 OWN이 설정된다")
  void nullRowScopeDefaultsToOwn() {
    PermissionAssignment assignment =
        new PermissionAssignment(FeatureCode.DRAFT, ActionCode.READ, null);
    assertThat(assignment.getRowScope()).isEqualTo(RowScope.OWN);
  }

  @Test
  @DisplayName("rowConditionExpression이 공백이면 null로 정규화된다")
  void blankConditionBecomesNull() {
    PermissionAssignment assignment =
        new PermissionAssignment(FeatureCode.DRAFT, ActionCode.READ, RowScope.OWN, "   ");
    assertThat(assignment.getRowConditionExpression()).isEmpty();
  }

  @Test
  @DisplayName("rowConditionExpression이 있으면 Optional로 반환된다")
  void conditionIsReturned() {
    PermissionAssignment assignment =
        new PermissionAssignment(FeatureCode.DRAFT, ActionCode.READ, RowScope.OWN, "age>10");
    assertThat(assignment.getRowConditionExpression()).contains("age>10");
  }
}
