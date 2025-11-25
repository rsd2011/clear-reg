package com.example.auth.permission.check;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.auth.domain.UserAccount;
import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.PermissionAssignment;
import com.example.auth.permission.PermissionGroup;
import com.example.common.security.RowScope;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PermissionEvaluationContextGettersTest {

  @Test
  @DisplayName("getter들이 생성자 인자를 그대로 반환한다")
  void gettersReturnValues() {
    UserAccount user =
        UserAccount.builder()
            .username("user")
            .password("pw")
            .organizationCode("ORG")
            .permissionGroupCode("PG")
            .build();
    PermissionGroup group = new PermissionGroup("PG", "PG-NAME");
    PermissionAssignment assignment =
        new PermissionAssignment(FeatureCode.DRAFT, ActionCode.READ, RowScope.ALL, "age>10");

    var ctx =
        new PermissionEvaluationContext(
            FeatureCode.DRAFT, ActionCode.READ, user, group, assignment, Map.of("row", 1));

    assertThat(ctx.feature()).isEqualTo(FeatureCode.DRAFT);
    assertThat(ctx.action()).isEqualTo(ActionCode.READ);
    assertThat(ctx.account()).isEqualTo(user);
    assertThat(ctx.group()).isEqualTo(group);
    assertThat(ctx.assignment()).isEqualTo(assignment);
    assertThat(ctx.attributes()).containsEntry("row", 1);
  }
}
