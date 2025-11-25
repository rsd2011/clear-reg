package com.example.auth.permission.check;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.auth.domain.UserAccount;
import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.PermissionAssignment;
import com.example.auth.permission.PermissionGroup;
import com.example.common.security.RowScope;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PermissionEvaluationContextImmutabilityTest {

  @Test
  @DisplayName("생성 후 원본 맵을 수정해도 attributes는 불변 사본을 유지한다")
  void attributesRemainUnchanged() {
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("row", 1);

    UserAccount user =
        UserAccount.builder()
            .username("user")
            .password("pw")
            .organizationCode("ORG")
            .permissionGroupCode("PG")
            .build();
    PermissionGroup group = new PermissionGroup("PG", "PG-NAME");
    PermissionAssignment assignment =
        new PermissionAssignment(FeatureCode.DRAFT, ActionCode.READ, RowScope.OWN);

    var ctx =
        new PermissionEvaluationContext(
            FeatureCode.DRAFT, ActionCode.READ, user, group, assignment, attrs);

    attrs.put("row", 99);

    assertThat(ctx.attributes()).containsEntry("row", 1);
    assertThat(ctx.attributes()).isUnmodifiable();
  }
}
