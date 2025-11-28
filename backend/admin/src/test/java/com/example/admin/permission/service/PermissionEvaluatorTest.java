package com.example.admin.permission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.example.admin.permission.TestUserInfo;
import com.example.admin.permission.check.RowConditionPermissionCheck;
import com.example.admin.permission.context.PermissionDecision;
import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.admin.permission.domain.PermissionAssignment;
import com.example.admin.permission.domain.PermissionGroup;
import com.example.admin.permission.exception.PermissionDeniedException;
import com.example.admin.permission.spi.OrganizationPolicyProvider;
import com.example.admin.permission.spi.UserInfo;
import com.example.admin.permission.spi.UserInfoProvider;
import com.example.common.security.RowScope;
import com.example.testing.bdd.Scenario;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionEvaluator 테스트")
class PermissionEvaluatorTest {

  @Mock private UserInfoProvider userInfoProvider;
  @Mock private PermissionGroupService permissionGroupService;
  @Mock private OrganizationPolicyProvider organizationPolicyProvider;
  @Mock private RowConditionEvaluator rowConditionEvaluator;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Given 인증 사용자가 권한을 가질 때 When evaluate 호출 Then PermissionDecision을 반환한다")
  void givenAuthenticatedUser_whenPermissionExists_thenReturnDecision() {
    PermissionEvaluator evaluator =
        new PermissionEvaluator(
            userInfoProvider,
            permissionGroupService,
            organizationPolicyProvider,
            List.of(new RowConditionPermissionCheck(rowConditionEvaluator)));
    UserInfo userInfo = new TestUserInfo("auditor", "ORG1", "AUDIT", Set.of("ROLE_AUDITOR"));
    PermissionGroup group = mock(PermissionGroup.class);
    PermissionAssignment assignment =
        new PermissionAssignment(FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ORG);
    given(group.getCode()).willReturn("AUDIT");
    given(group.getName()).willReturn("Auditor");
    given(group.getDefaultRowScope()).willReturn(RowScope.ORG);

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("auditor", "token", java.util.List.of()));
    given(userInfoProvider.getByUsernameOrThrow("auditor")).willReturn(userInfo);
    given(permissionGroupService.getByCodeOrThrow("AUDIT")).willReturn(group);
    given(group.assignmentFor(FeatureCode.ORGANIZATION, ActionCode.READ))
        .willReturn(Optional.of(assignment));
    given(rowConditionEvaluator.isAllowed(any(), any())).willReturn(true);

    Scenario.given("권한 평가", () -> evaluator.evaluate(FeatureCode.ORGANIZATION, ActionCode.READ))
        .then(
            "AuthContext 구성",
            decision -> {
              assertThat(decision).isInstanceOf(PermissionDecision.class);
              assertThat(decision.toContext().username()).isEqualTo("auditor");
              assertThat(decision.toContext().rowScope()).isEqualTo(RowScope.ORG);
            });
  }

  @Test
  @DisplayName("Given 인증 컨텍스트가 없을 때 When evaluate 호출 Then PermissionDeniedException을 던진다")
  void givenNoAuthentication_whenEvaluating_thenThrows() {
    PermissionEvaluator evaluator =
        new PermissionEvaluator(
            userInfoProvider,
            permissionGroupService,
            organizationPolicyProvider,
            List.of(new RowConditionPermissionCheck(rowConditionEvaluator)));

    assertThatThrownBy(() -> evaluator.evaluate(FeatureCode.CUSTOMER, ActionCode.READ))
        .isInstanceOf(PermissionDeniedException.class);
  }

  @Test
  @DisplayName("Given 그룹 코드가 없을 때 When evaluate 호출 Then 조직 기본 그룹을 사용한다")
  void givenMissingPermissionGroup_whenEvaluating_thenUsesOrganizationDefault() {
    PermissionEvaluator evaluator =
        new PermissionEvaluator(
            userInfoProvider,
            permissionGroupService,
            organizationPolicyProvider,
            List.of(new RowConditionPermissionCheck(rowConditionEvaluator)));
    TestUserInfo userInfo = new TestUserInfo("auditor", "ORG2", "DEFAULT", Set.of("ROLE_AUDITOR"));
    userInfo.setPermissionGroupCode(null);
    PermissionGroup group = mock(PermissionGroup.class);
    PermissionAssignment assignment =
        new PermissionAssignment(FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ORG);
    given(group.getCode()).willReturn("POLICY_ORG2");
    given(group.getName()).willReturn("Policy Group");
    given(group.getDefaultRowScope()).willReturn(RowScope.ORG);

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("auditor", "token", java.util.List.of()));
    given(userInfoProvider.getByUsernameOrThrow("auditor")).willReturn(userInfo);
    given(organizationPolicyProvider.defaultPermissionGroup("ORG2")).willReturn("POLICY_ORG2");
    given(permissionGroupService.getByCodeOrThrow("POLICY_ORG2")).willReturn(group);
    given(group.assignmentFor(FeatureCode.ORGANIZATION, ActionCode.READ))
        .willReturn(Optional.of(assignment));
    given(rowConditionEvaluator.isAllowed(any(), any())).willReturn(true);

    PermissionDecision decision = evaluator.evaluate(FeatureCode.ORGANIZATION, ActionCode.READ);

    assertThat(decision.assignment().getRowScope()).isEqualTo(RowScope.ORG);
  }

  @Test
  @DisplayName("Given 행 조건이 불만족일 때 When evaluate 호출 Then PermissionDeniedException을 던진다")
  void givenConditionNotSatisfied_whenEvaluating_thenThrows() {
    PermissionEvaluator evaluator =
        new PermissionEvaluator(
            userInfoProvider,
            permissionGroupService,
            organizationPolicyProvider,
            List.of(new RowConditionPermissionCheck(rowConditionEvaluator)));
    UserInfo userInfo = new TestUserInfo("analyst", "ORG3", "ANALYST", Set.of("ROLE_ANALYST"));
    PermissionGroup group = mock(PermissionGroup.class);
    PermissionAssignment assignment =
        new PermissionAssignment(FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ORG);
    given(group.getCode()).willReturn("ANALYST");
    given(group.getName()).willReturn("Analyst");
    given(group.getDefaultRowScope()).willReturn(RowScope.ORG);

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("analyst", "token", java.util.List.of()));
    given(userInfoProvider.getByUsernameOrThrow("analyst")).willReturn(userInfo);
    given(permissionGroupService.getByCodeOrThrow("ANALYST")).willReturn(group);
    given(group.assignmentFor(FeatureCode.ORGANIZATION, ActionCode.READ))
        .willReturn(Optional.of(assignment));
    given(rowConditionEvaluator.isAllowed(any(), any())).willReturn(false);

    assertThatThrownBy(() -> evaluator.evaluate(FeatureCode.ORGANIZATION, ActionCode.READ))
        .isInstanceOf(PermissionDeniedException.class)
        .hasMessageContaining("행 조건");
  }
}
