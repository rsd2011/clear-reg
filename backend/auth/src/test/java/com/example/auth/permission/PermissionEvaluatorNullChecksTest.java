package com.example.auth.permission;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountService;
import com.example.auth.organization.OrganizationPolicyService;
import com.example.common.security.RowScope;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionEvaluatorNullChecksTest {

    @Mock UserAccountService userAccountService;
    @Mock PermissionGroupService groupService;
    @Mock OrganizationPolicyService orgPolicyService;

    @Test
    @DisplayName("permissionChecks가 null이어도 인증이 있으면 평가를 수행한다")
    void nullPermissionChecksHandled() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "pw", "ROLE_USER"));
        PermissionEvaluator evaluator = new PermissionEvaluator(userAccountService, groupService, orgPolicyService, null);

        UserAccount user = UserAccount.builder()
                .username("user")
                .password("p")
                .organizationCode("ORG")
                .permissionGroupCode("PG")
                .build();
        PermissionGroup group = new PermissionGroup("PG", "group");
        group.replaceAssignments(List.of(new PermissionAssignment(FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.OWN, null)));

        given(userAccountService.getByUsernameOrThrow("user")).willReturn(user);
        given(groupService.getByCodeOrThrow("PG")).willReturn(group);
        given(orgPolicyService.defaultPermissionGroup("ORG")).willReturn(null);

        assertThatCode(() -> evaluator.evaluate(FeatureCode.ORGANIZATION, ActionCode.READ)).doesNotThrowAnyException();
    }
}
