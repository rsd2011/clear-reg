package com.example.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CurrentUserTest {

    @Test
    @DisplayName("CurrentUser 생성 시 모든 필드가 올바르게 설정된다")
    void createCurrentUserWithAllFields() {
        CurrentUser user = new CurrentUser(
                "testUser",
                "ORG1",
                "ADMIN_GROUP",
                "ORGANIZATION",
                "READ",
                RowScope.ORG,
                100L,
                List.of("GROUP1", "GROUP2"),
                "HR"
        );

        assertThat(user.username()).isEqualTo("testUser");
        assertThat(user.organizationCode()).isEqualTo("ORG1");
        assertThat(user.permissionGroupCode()).isEqualTo("ADMIN_GROUP");
        assertThat(user.featureCode()).isEqualTo("ORGANIZATION");
        assertThat(user.actionCode()).isEqualTo("READ");
        assertThat(user.rowScope()).isEqualTo(RowScope.ORG);
        assertThat(user.orgPolicyId()).isEqualTo(100L);
        assertThat(user.orgGroupCodes()).containsExactly("GROUP1", "GROUP2");
        assertThat(user.businessType()).isEqualTo("HR");
    }

    @Test
    @DisplayName("CurrentUser는 null 값도 허용한다")
    void createCurrentUserWithNullValues() {
        CurrentUser user = new CurrentUser(
                "user",
                "ORG",
                null,
                null,
                null,
                RowScope.OWN,
                null,
                List.of(),
                null
        );

        assertThat(user.username()).isEqualTo("user");
        assertThat(user.orgPolicyId()).isNull();
        assertThat(user.orgGroupCodes()).isEmpty();
        assertThat(user.businessType()).isNull();
    }
}
