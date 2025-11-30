package com.example.admin.permission.domain;

import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PermissionAssignment 테스트.
 *
 * <p>RowScope와 rowConditionExpression은 RowAccessPolicy로 이관되어 제거되었습니다.
 */
class PermissionAssignmentTest {

  @Test
  @DisplayName("Given feature, action When 생성 Then feature, action이 설정된다")
  void givenFeatureAction_whenCreate_thenFieldsAreSet() {
    PermissionAssignment assignment =
        new PermissionAssignment(FeatureCode.DRAFT, ActionCode.READ);

    assertThat(assignment.getFeature()).isEqualTo(FeatureCode.DRAFT);
    assertThat(assignment.getAction()).isEqualTo(ActionCode.READ);
  }

  @Test
  @DisplayName("Given 동일한 feature, action When equals 호출 Then true 반환")
  void givenSameFeatureAction_whenEquals_thenReturnsTrue() {
    PermissionAssignment assignment1 =
        new PermissionAssignment(FeatureCode.DRAFT, ActionCode.READ);
    PermissionAssignment assignment2 =
        new PermissionAssignment(FeatureCode.DRAFT, ActionCode.READ);

    assertThat(assignment1).isEqualTo(assignment2);
    assertThat(assignment1.hashCode()).isEqualTo(assignment2.hashCode());
  }

  @Test
  @DisplayName("Given 다른 feature When equals 호출 Then false 반환")
  void givenDifferentFeature_whenEquals_thenReturnsFalse() {
    PermissionAssignment assignment1 =
        new PermissionAssignment(FeatureCode.DRAFT, ActionCode.READ);
    PermissionAssignment assignment2 =
        new PermissionAssignment(FeatureCode.APPROVAL, ActionCode.READ);

    assertThat(assignment1).isNotEqualTo(assignment2);
  }

  @Test
  @DisplayName("Given 다른 action When equals 호출 Then false 반환")
  void givenDifferentAction_whenEquals_thenReturnsFalse() {
    PermissionAssignment assignment1 =
        new PermissionAssignment(FeatureCode.DRAFT, ActionCode.READ);
    PermissionAssignment assignment2 =
        new PermissionAssignment(FeatureCode.DRAFT, ActionCode.UPDATE);

    assertThat(assignment1).isNotEqualTo(assignment2);
  }
}
