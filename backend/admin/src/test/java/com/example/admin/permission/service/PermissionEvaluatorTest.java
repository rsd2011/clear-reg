package com.example.admin.permission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.example.admin.permission.TestUserInfo;
import com.example.admin.permission.context.PermissionDecision;
import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.admin.permission.domain.PermissionAssignment;
import com.example.admin.permission.domain.PermissionGroup;
import com.example.admin.permission.exception.PermissionDeniedException;
import com.example.common.user.spi.UserAccountInfo;
import com.example.common.user.spi.UserAccountProvider;
import com.example.testing.bdd.Scenario;
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

  @Mock private UserAccountProvider userAccountProvider;
  @Mock private PermissionGroupService permissionGroupService;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Given 인증 사용자가 권한을 가질 때 When evaluate 호출 Then PermissionDecision을 반환한다")
  void givenAuthenticatedUser_whenPermissionExists_thenReturnDecision() {
    PermissionEvaluator evaluator =
        new PermissionEvaluator(userAccountProvider, permissionGroupService);
    UserAccountInfo userInfo = new TestUserInfo("auditor", "ORG1", "AUDIT", Set.of("ROLE_AUDITOR"));
    PermissionGroup group = mock(PermissionGroup.class);
    PermissionAssignment assignment =
        new PermissionAssignment(FeatureCode.ORGANIZATION, ActionCode.READ);

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("auditor", "token", java.util.List.of()));
    given(userAccountProvider.getByUsernameOrThrow("auditor")).willReturn(userInfo);
    given(permissionGroupService.getByCodeOrThrow("AUDIT")).willReturn(group);
    given(group.assignmentFor(FeatureCode.ORGANIZATION, ActionCode.READ))
        .willReturn(Optional.of(assignment));

    Scenario.given("권한 평가", () -> evaluator.evaluate(FeatureCode.ORGANIZATION, ActionCode.READ))
        .then(
            "AuthContext 구성",
            decision -> {
              assertThat(decision).isInstanceOf(PermissionDecision.class);
              assertThat(decision.toContext().username()).isEqualTo("auditor");
              assertThat(decision.toContext().feature()).isEqualTo(FeatureCode.ORGANIZATION);
              assertThat(decision.toContext().action()).isEqualTo(ActionCode.READ);
            });
  }

  @Test
  @DisplayName("Given 인증 컨텍스트가 없을 때 When evaluate 호출 Then PermissionDeniedException을 던진다")
  void givenNoAuthentication_whenEvaluating_thenThrows() {
    PermissionEvaluator evaluator =
        new PermissionEvaluator(userAccountProvider, permissionGroupService);

    assertThatThrownBy(() -> evaluator.evaluate(FeatureCode.CUSTOMER, ActionCode.READ))
        .isInstanceOf(PermissionDeniedException.class);
  }

  @Test
  @DisplayName("Given 그룹 코드가 없을 때 When evaluate 호출 Then DEFAULT 그룹을 사용한다")
  void givenMissingPermissionGroup_whenEvaluating_thenUsesDefaultGroup() {
    PermissionEvaluator evaluator =
        new PermissionEvaluator(userAccountProvider, permissionGroupService);
    TestUserInfo userInfo = new TestUserInfo("auditor", "ORG2", "DEFAULT", Set.of("ROLE_AUDITOR"));
    userInfo.setPermissionGroupCode(null);
    PermissionGroup group = mock(PermissionGroup.class);
    PermissionAssignment assignment =
        new PermissionAssignment(FeatureCode.ORGANIZATION, ActionCode.READ);

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("auditor", "token", java.util.List.of()));
    given(userAccountProvider.getByUsernameOrThrow("auditor")).willReturn(userInfo);
    given(permissionGroupService.getByCodeOrThrow("DEFAULT")).willReturn(group);
    given(group.assignmentFor(FeatureCode.ORGANIZATION, ActionCode.READ))
        .willReturn(Optional.of(assignment));

    PermissionDecision decision = evaluator.evaluate(FeatureCode.ORGANIZATION, ActionCode.READ);

    assertThat(decision.assignment().getFeature()).isEqualTo(FeatureCode.ORGANIZATION);
    assertThat(decision.assignment().getAction()).isEqualTo(ActionCode.READ);
  }

  @Test
  @DisplayName("Given 권한 할당이 없을 때 When evaluate 호출 Then PermissionDeniedException을 던진다")
  void givenNoAssignment_whenEvaluating_thenThrows() {
    PermissionEvaluator evaluator =
        new PermissionEvaluator(userAccountProvider, permissionGroupService);
    UserAccountInfo userInfo = new TestUserInfo("analyst", "ORG3", "ANALYST", Set.of("ROLE_ANALYST"));
    PermissionGroup group = mock(PermissionGroup.class);

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("analyst", "token", java.util.List.of()));
    given(userAccountProvider.getByUsernameOrThrow("analyst")).willReturn(userInfo);
    given(permissionGroupService.getByCodeOrThrow("ANALYST")).willReturn(group);
    given(group.assignmentFor(FeatureCode.ORGANIZATION, ActionCode.READ))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> evaluator.evaluate(FeatureCode.ORGANIZATION, ActionCode.READ))
        .isInstanceOf(PermissionDeniedException.class)
        .hasMessageContaining("권한이 없습니다");
  }
}
