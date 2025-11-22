package com.example.auth.permission;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.example.auth.permission.check.PermissionCheck;
import com.example.auth.permission.check.PermissionEvaluationContext;
import com.example.common.security.RowScope;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionEvaluatorChainFailureTest {

    @Mock UserAccountService userAccountService;
    @Mock PermissionGroupService groupService;
    @Mock OrganizationPolicyService orgPolicyService;
    @Mock PermissionCheck passCheck;
    @Mock PermissionCheck failCheck;

    @Test
    @DisplayName("다수 PermissionCheck 중 이후 체크가 예외를 던지면 전파된다")
    void secondPermissionCheckThrows() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "pw"));
        PermissionEvaluator evaluator = new PermissionEvaluator(
                userAccountService, groupService, orgPolicyService, List.of(passCheck, failCheck));

        UserAccount user = UserAccount.builder()
                .username("user")
                .password("p")
                .organizationCode("ORG")
                .permissionGroupCode("GROUP")
                .build();
        PermissionGroup group = new PermissionGroup("GROUP", "그룹");
        group.replaceAssignments(List.of(new PermissionAssignment(FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.OWN, null)));

        given(userAccountService.getByUsernameOrThrow("user")).willReturn(user);
        given(groupService.getByCodeOrThrow("GROUP")).willReturn(group);
        given(orgPolicyService.defaultPermissionGroup("ORG")).willReturn(null);
        // 첫 체크는 통과
        // 첫 체크는 통과(아무 동작 없음)
        org.mockito.Mockito.doNothing().when(passCheck).check(org.mockito.ArgumentMatchers.any(PermissionEvaluationContext.class));
        // 두 번째 체크에서 실패
        org.mockito.Mockito.doThrow(new PermissionDeniedException("denied"))
                .when(failCheck).check(org.mockito.ArgumentMatchers.any(PermissionEvaluationContext.class));

        assertThatThrownBy(() -> evaluator.evaluate(FeatureCode.ORGANIZATION, ActionCode.READ))
                .isInstanceOf(PermissionDeniedException.class);
    }
}
