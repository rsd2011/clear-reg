package com.example.auth.permission;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountService;
import com.example.auth.organization.OrganizationPolicyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionEvaluatorMissingAssignmentTest {

  @Mock UserAccountService userAccountService;
  @Mock PermissionGroupService groupService;
  @Mock OrganizationPolicyService orgPolicyService;

  @Test
  @DisplayName("권한 매핑이 없으면 PermissionDeniedException을 던진다")
  void throwsWhenAssignmentMissing() {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("user", "pw"));
    PermissionEvaluator evaluator =
        new PermissionEvaluator(
            userAccountService, groupService, orgPolicyService, java.util.List.of());

    UserAccount user =
        UserAccount.builder()
            .username("user")
            .password("p")
            .organizationCode("ORG")
            .permissionGroupCode("GROUP")
            .build();
    PermissionGroup emptyGroup = new PermissionGroup("GROUP", "그룹");

    given(userAccountService.getByUsernameOrThrow("user")).willReturn(user);
    given(groupService.getByCodeOrThrow("GROUP")).willReturn(emptyGroup);

    assertThatThrownBy(() -> evaluator.evaluate(FeatureCode.ORGANIZATION, ActionCode.READ))
        .isInstanceOf(PermissionDeniedException.class);
  }
}
