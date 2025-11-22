package com.example.auth.permission;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionEvaluatorNoDefaultGroupTest {

    @Mock UserAccountService userAccountService;
    @Mock PermissionGroupService groupService;
    @Mock OrganizationPolicyService orgPolicyService;

    @Test
    @DisplayName("기본 그룹이 없으면 PermissionDeniedException을 던진다")
    void throwsWhenNoDefaultGroup() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "pw"));
        PermissionEvaluator evaluator = new PermissionEvaluator(userAccountService, groupService, orgPolicyService, java.util.List.of());

        UserAccount user = UserAccount.builder().username("user").password("p").organizationCode("ORG").permissionGroupCode(null).build();
        given(userAccountService.getByUsernameOrThrow("user")).willReturn(user);
        given(orgPolicyService.defaultPermissionGroup("ORG")).willReturn(null);

        assertThatThrownBy(() -> evaluator.evaluate(FeatureCode.ORGANIZATION, ActionCode.READ))
                .isInstanceOf(PermissionDeniedException.class);
    }
}
