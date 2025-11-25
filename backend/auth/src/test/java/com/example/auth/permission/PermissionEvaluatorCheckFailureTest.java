package com.example.auth.permission;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountService;
import com.example.auth.organization.OrganizationPolicyService;
import com.example.auth.permission.check.PermissionCheck;
import com.example.auth.permission.check.PermissionEvaluationContext;
import com.example.common.security.RowScope;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionEvaluatorCheckFailureTest {

  @Mock UserAccountService userAccountService;
  @Mock PermissionGroupService groupService;
  @Mock OrganizationPolicyService orgPolicyService;
  @Mock PermissionCheck check;

  @Test
  @DisplayName("PermissionCheck가 예외를 던지면 PermissionDeniedException으로 전파된다")
  void permissionCheckFailurePropagates() {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("user", "pw"));
    PermissionEvaluator evaluator =
        new PermissionEvaluator(
            userAccountService, groupService, orgPolicyService, java.util.List.of(check));

    UserAccount user =
        UserAccount.builder()
            .username("user")
            .password("p")
            .organizationCode("ORG")
            .permissionGroupCode("GROUP")
            .build();
    PermissionGroup group = new PermissionGroup("GROUP", "그룹");
    group.replaceAssignments(
        List.of(
            new PermissionAssignment(
                FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.OWN, null)));

    given(userAccountService.getByUsernameOrThrow("user")).willReturn(user);
    given(groupService.getByCodeOrThrow("GROUP")).willReturn(group);
    Mockito.doThrow(new PermissionDeniedException("denied"))
        .when(check)
        .check(Mockito.any(PermissionEvaluationContext.class));

    assertThatThrownBy(() -> evaluator.evaluate(FeatureCode.ORGANIZATION, ActionCode.READ))
        .isInstanceOf(PermissionDeniedException.class);
  }
}
