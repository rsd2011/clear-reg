package com.example.admin.permission.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.common.security.RowScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthContextCoverageTest {

    @Test
    @DisplayName("AuthContext.of() 팩토리 메서드는 기본값으로 생성한다")
    void ofFactoryCreatesWithDefaults() {
        AuthContext ctx = AuthContext.of("u", "org", "grp", FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ALL);

        assertThat(ctx.username()).isEqualTo("u");
        assertThat(ctx.organizationCode()).isEqualTo("org");
        assertThat(ctx.permissionGroupCode()).isEqualTo("grp");
        assertThat(ctx.feature()).isEqualTo(FeatureCode.ORGANIZATION);
        assertThat(ctx.action()).isEqualTo(ActionCode.READ);
        assertThat(ctx.rowScope()).isEqualTo(RowScope.ALL);
        assertThat(ctx.orgPolicyId()).isNull();
        assertThat(ctx.orgGroupCodes()).isEmpty();
        assertThat(ctx.businessType()).isNull();
    }

    @Test
    @DisplayName("AuthContext 전체 생성자는 모든 필드를 설정한다")
    void fullConstructorSetsAllFields() {
        AuthContext ctx = new AuthContext("u", "org", "grp",
                FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ORG,
                100L, List.of("GROUP1", "GROUP2"), "HR");

        assertThat(ctx.orgPolicyId()).isEqualTo(100L);
        assertThat(ctx.orgGroupCodes()).containsExactly("GROUP1", "GROUP2");
        assertThat(ctx.businessType()).isEqualTo("HR");
    }

    @Test
    @DisplayName("orgGroupCodes는 null일 때 빈 리스트로 정규화된다")
    void orgGroupCodesNormalizedToEmptyList() {
        AuthContext ctx = new AuthContext("u", "org", "grp",
                FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ORG,
                null, null, null);

        assertThat(ctx.orgGroupCodes()).isNotNull().isEmpty();
    }
}
