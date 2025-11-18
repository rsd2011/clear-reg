package com.example.auth.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountService;
import com.example.auth.permission.context.PermissionDecision;
import com.example.common.security.RowScope;
import com.example.testing.bdd.Scenario;

@ExtendWith(MockitoExtension.class)
class PermissionEvaluatorTest {

    @Mock
    private UserAccountService userAccountService;
    @Mock
    private PermissionGroupService permissionGroupService;
    @Mock
    private com.example.auth.organization.OrganizationPolicyService organizationPolicyService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenAuthenticatedUser_whenPermissionExists_thenReturnDecision() {
        PermissionEvaluator evaluator = new PermissionEvaluator(userAccountService, permissionGroupService, organizationPolicyService);
        UserAccount account = UserAccount.builder()
                .username("auditor")
                .password("encoded")
                .roles(Set.of("ROLE_AUDITOR"))
                .organizationCode("ORG1")
                .permissionGroupCode("AUDIT")
                .build();
        PermissionGroup group = mock(PermissionGroup.class);
        PermissionAssignment assignment = new PermissionAssignment(FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ORG);
        FieldMaskRule maskRule = new FieldMaskRule("ORG_NAME", "***", ActionCode.UNMASK, true);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("auditor", "token", java.util.List.of()));
        given(userAccountService.getByUsernameOrThrow("auditor")).willReturn(account);
        given(permissionGroupService.getByCodeOrThrow("AUDIT")).willReturn(group);
        given(group.assignmentFor(FeatureCode.ORGANIZATION, ActionCode.READ)).willReturn(Optional.of(assignment));
        given(group.maskRulesByTag()).willReturn(Map.of("ORG_NAME", maskRule));

        Scenario.given("권한 평가", () -> evaluator.evaluate(FeatureCode.ORGANIZATION, ActionCode.READ))
                .then("AuthContext 구성", decision -> {
                    assertThat(decision).isInstanceOf(PermissionDecision.class);
                    assertThat(decision.toContext().username()).isEqualTo("auditor");
                    assertThat(decision.toContext().rowScope()).isEqualTo(RowScope.ORG);
                });
    }

    @Test
    void givenNoAuthentication_whenEvaluating_thenThrows() {
        PermissionEvaluator evaluator = new PermissionEvaluator(userAccountService, permissionGroupService, organizationPolicyService);

        assertThatThrownBy(() -> evaluator.evaluate(FeatureCode.CUSTOMER, ActionCode.READ))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void givenMissingPermissionGroup_whenEvaluating_thenUsesOrganizationDefault() {
        PermissionEvaluator evaluator = new PermissionEvaluator(userAccountService, permissionGroupService, organizationPolicyService);
        UserAccount account = UserAccount.builder()
                .username("auditor")
                .password("encoded")
                .roles(Set.of("ROLE_AUDITOR"))
                .organizationCode("ORG2")
                .permissionGroupCode("DEFAULT")
                .build();
        account.updatePermissionGroupCode(null);
        PermissionGroup group = mock(PermissionGroup.class);
        PermissionAssignment assignment = new PermissionAssignment(FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ORG);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("auditor", "token", java.util.List.of()));
        given(userAccountService.getByUsernameOrThrow("auditor")).willReturn(account);
        given(organizationPolicyService.defaultPermissionGroup("ORG2")).willReturn("POLICY_ORG2");
        given(permissionGroupService.getByCodeOrThrow("POLICY_ORG2")).willReturn(group);
        given(group.assignmentFor(FeatureCode.ORGANIZATION, ActionCode.READ)).willReturn(Optional.of(assignment));

        PermissionDecision decision = evaluator.evaluate(FeatureCode.ORGANIZATION, ActionCode.READ);

        assertThat(decision.assignment().getRowScope()).isEqualTo(RowScope.ORG);
    }
}
