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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionEvaluatorDefaultGroupMissingTest {

    @Mock UserAccountService userAccountService;
    @Mock PermissionGroupService groupService;
    @Mock OrganizationPolicyService orgPolicyService;

    @Test
    @DisplayName("기본 그룹 코드가 비어있고 그룹 조회에 실패하면 PermissionDeniedException")
    void emptyDefaultGroupThenDenied() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "pw"));
        PermissionEvaluator evaluator = new PermissionEvaluator(userAccountService, groupService, orgPolicyService, List.of());

        UserAccount user = UserAccount.builder()
                .username("user")
                .password("p")
                .organizationCode("ORG")
                .permissionGroupCode(null)
                .build();

        given(userAccountService.getByUsernameOrThrow("user")).willReturn(user);
        given(orgPolicyService.defaultPermissionGroup("ORG")).willReturn("");
        given(groupService.getByCodeOrThrow("")).willThrow(new PermissionDeniedException("no group"));

        assertThatThrownBy(() -> evaluator.evaluate(FeatureCode.ORGANIZATION, ActionCode.READ))
                .isInstanceOf(PermissionDeniedException.class);
    }
}
