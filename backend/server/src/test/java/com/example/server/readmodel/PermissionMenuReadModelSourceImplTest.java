package com.example.server.readmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.admin.menu.domain.MenuCapability;
import com.example.common.user.spi.UserAccountInfo;
import com.example.common.user.spi.UserAccountProvider;
import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.admin.permission.domain.PermissionAssignment;
import com.example.admin.permission.domain.PermissionGroup;
import com.example.admin.permission.service.PermissionGroupService;
import com.example.admin.permission.service.PermissionMenuService;
import com.example.admin.permission.service.PermissionMenuService.MenuTreeNode;
import com.example.dw.application.readmodel.PermissionMenuReadModel;

class PermissionMenuReadModelSourceImplTest {

    private final UserAccountProvider userAccountProvider = Mockito.mock(UserAccountProvider.class);
    private final PermissionGroupService permissionGroupService = Mockito.mock(PermissionGroupService.class);
    private final PermissionMenuService permissionMenuService = Mockito.mock(PermissionMenuService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

    private PermissionMenuReadModelSourceImpl source;

    @BeforeEach
    void setUp() {
        source = new PermissionMenuReadModelSourceImpl(userAccountProvider, permissionGroupService, permissionMenuService, clock);
    }

    @Test
    void buildsMenuFromPermissionGroupAssignments() {
        UserAccountInfo account = createMockUserAccountInfo("user1", "ORG1", "GROUP1");
        when(userAccountProvider.getByUsernameOrThrow("user1")).thenReturn(account);

        PermissionGroup group = Mockito.mock(PermissionGroup.class);
        given(group.getCode()).willReturn("GROUP1");
        PermissionAssignment assignment = new PermissionAssignment(FeatureCode.FILE, ActionCode.READ);
        given(group.getAssignments()).willReturn(List.of(assignment));
        // assignmentFor()는 PermissionMenuReadModelSourceImpl에서 메뉴 필터링에 사용됨
        given(group.assignmentFor(FeatureCode.FILE, ActionCode.READ)).willReturn(java.util.Optional.of(assignment));
        when(permissionGroupService.getByCodeOrThrow("GROUP1")).thenReturn(group);

        // PermissionMenuService mock setup
        MenuCapability capability = new MenuCapability(FeatureCode.FILE, ActionCode.READ);
        MenuTreeNode menuNode = new MenuTreeNode(
                UUID.randomUUID(),
                "FILE_MENU",
                "파일 관리",
                "/file",
                null,
                1,
                false,
                Set.of(capability),
                List.of()
        );
        when(permissionMenuService.getMenuTree("GROUP1")).thenReturn(List.of(menuNode));

        PermissionMenuReadModel model = source.snapshot("user1");

        assertThat(model.items()).hasSize(1);
        assertThat(model.items().get(0).featureCode()).isEqualTo("FILE");
        assertThat(model.items().get(0).actionCode()).isEqualTo("READ");
        assertThat(model.items().get(0).path()).isEqualTo("/file");
    }

    @Test
    void fallsBackToDefaultGroupWhenAccountHasNoGroup() {
        UserAccountInfo account = createMockUserAccountInfo("user2", "ORG2", null);
        when(userAccountProvider.getByUsernameOrThrow("user2")).thenReturn(account);

        PermissionGroup group = Mockito.mock(PermissionGroup.class);
        given(group.getCode()).willReturn("DEFAULT");
        given(group.getAssignments()).willReturn(List.of());
        when(permissionGroupService.getByCodeOrThrow("DEFAULT")).thenReturn(group);

        // Empty menu tree
        when(permissionMenuService.getMenuTree("DEFAULT")).thenReturn(List.of());

        PermissionMenuReadModel model = source.snapshot("user2");

        assertThat(model.items()).isEmpty();
    }

    private UserAccountInfo createMockUserAccountInfo(String username, String orgCode, String permGroupCode) {
        UserAccountInfo user = mock(UserAccountInfo.class);
        given(user.getUsername()).willReturn(username);
        given(user.getOrganizationCode()).willReturn(orgCode);
        given(user.getPermissionGroupCode()).willReturn(permGroupCode);
        return user;
    }
}
