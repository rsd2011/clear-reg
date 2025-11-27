package com.example.admin.permission;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.common.security.RowScope;
import java.lang.reflect.Field;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PermissionGroup 도메인 테스트")
class PermissionGroupTest {

  @Test
  @DisplayName("Given 권한/마스킹 규칙 When 조회하면 Then 해당 규칙이 반환된다")
  void givenAssignments_whenLookup_thenReturnMatchingPermission() throws Exception {
    PermissionGroup group = new PermissionGroup("AUDIT", "Auditor");
    Field assignments = PermissionGroup.class.getDeclaredField("assignments");
    assignments.setAccessible(true);
    @SuppressWarnings("unchecked")
    Set<PermissionAssignment> values = (Set<PermissionAssignment>) assignments.get(group);
    PermissionAssignment assignment =
        new PermissionAssignment(FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ALL);
    values.add(assignment);

    Field maskField = PermissionGroup.class.getDeclaredField("maskRules");
    maskField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Set<FieldMaskRule> maskRules = (Set<FieldMaskRule>) maskField.get(group);
    maskRules.add(new FieldMaskRule("ORG_NAME", "***", ActionCode.UNMASK, true));

    assertThat(group.assignmentFor(FeatureCode.ORGANIZATION, ActionCode.READ)).contains(assignment);
    assertThat(group.maskRulesByTag()).containsKey("ORG_NAME");
  }
}
