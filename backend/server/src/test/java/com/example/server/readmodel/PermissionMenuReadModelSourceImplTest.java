package com.example.server.readmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountService;
import com.example.auth.organization.OrganizationPolicyService;
import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.PermissionAssignment;
import com.example.auth.permission.PermissionGroup;
import com.example.auth.permission.PermissionGroupService;
import com.example.dw.application.readmodel.PermissionMenuReadModel;

class PermissionMenuReadModelSourceImplTest {

    private final UserAccountService userAccountService = Mockito.mock(UserAccountService.class);
    private final PermissionGroupService permissionGroupService = Mockito.mock(PermissionGroupService.class);
    private final OrganizationPolicyService organizationPolicyService = Mockito.mock(OrganizationPolicyService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

    private PermissionMenuReadModelSourceImpl source;

    @BeforeEach
    void setUp() {
        source = new PermissionMenuReadModelSourceImpl(userAccountService, permissionGroupService, organizationPolicyService, clock);
    }

    @Test
    void buildsMenuFromPermissionGroupAssignments() {
        UserAccount account = UserAccount.builder()
                .username("user1")
                .password("pw")
                .organizationCode("ORG1")
                .permissionGroupCode("GROUP1")
                .build();
        when(userAccountService.getByUsernameOrThrow("user1")).thenReturn(account);

        PermissionGroup group = new PermissionGroup("GROUP1", "Group");
        PermissionAssignment assignment = new PermissionAssignment(FeatureCode.FILE, ActionCode.READ, com.example.common.security.RowScope.ORG);
        group.replaceAssignments(List.of(assignment));
        group.replaceMaskRules(Set.of());
        when(permissionGroupService.getByCodeOrThrow("GROUP1")).thenReturn(group);

        PermissionMenuReadModel model = source.snapshot("user1");

        assertThat(model.items()).hasSize(1);
        assertThat(model.items().get(0).featureCode()).isEqualTo("FILE");
        assertThat(model.items().get(0).actionCode()).isEqualTo("READ");
        assertThat(model.items().get(0).path()).isEqualTo("/file");
    }

    @Test
    void fallsBackToOrgDefaultGroupWhenAccountHasNoGroup() {
        UserAccount account = UserAccount.builder()
                .username("user2")
                .password("pw")
                .organizationCode("ORG2")
                .permissionGroupCode(null)
                .build();
        when(userAccountService.getByUsernameOrThrow("user2")).thenReturn(account);
        when(organizationPolicyService.defaultPermissionGroup("ORG2")).thenReturn("DEFAULT");

        PermissionGroup group = new PermissionGroup("DEFAULT", "Default");
        group.replaceAssignments(List.of());
        group.replaceMaskRules(Set.of());
        when(permissionGroupService.getByCodeOrThrow("DEFAULT")).thenReturn(group);

        PermissionMenuReadModel model = source.snapshot("user2");

        assertThat(model.items()).isEmpty();
    }
}
