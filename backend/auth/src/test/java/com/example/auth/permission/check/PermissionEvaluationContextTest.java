package com.example.auth.permission.check;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.auth.domain.UserAccount;
import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.PermissionAssignment;
import com.example.auth.permission.PermissionGroup;
import com.example.common.security.RowScope;

class PermissionEvaluationContextTest {

    @Test
    @DisplayName("attributes는 불변 사본으로 보존된다")
    void attributesAreCopiedImmutably() {
        UserAccount user = UserAccount.builder()
                .username("user")
                .password("pw")
                .organizationCode("ORG")
                .permissionGroupCode("PG")
                .build();
        PermissionGroup group = new PermissionGroup("PG", "PG-NAME");
        PermissionAssignment assignment = new PermissionAssignment(FeatureCode.DRAFT, ActionCode.READ, RowScope.ORG);

        var context = new PermissionEvaluationContext(FeatureCode.DRAFT, ActionCode.READ, user, group, assignment, Map.of("row", 1));

        assertThat(context.attributes()).containsEntry("row", 1);
        assertThat(context.attributes()).isUnmodifiable();
    }
}
