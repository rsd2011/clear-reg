package com.example.server.readmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.admin.menu.domain.MenuDefinition;
import com.example.admin.menu.service.MenuDefinitionLoader;
import com.example.admin.menu.domain.MenuDefinitions;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountService;
import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.admin.permission.domain.PermissionAssignment;
import com.example.admin.permission.domain.PermissionGroup;
import com.example.admin.permission.service.PermissionGroupService;
import com.example.dw.application.readmodel.PermissionMenuReadModel;

class PermissionMenuReadModelSourceImplTest {

    private final UserAccountService userAccountService = Mockito.mock(UserAccountService.class);
    private final PermissionGroupService permissionGroupService = Mockito.mock(PermissionGroupService.class);
    private final MenuDefinitionLoader menuDefinitionLoader = Mockito.mock(MenuDefinitionLoader.class);
    private final Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

    private PermissionMenuReadModelSourceImpl source;

    @BeforeEach
    void setUp() {
        source = new PermissionMenuReadModelSourceImpl(userAccountService, permissionGroupService, menuDefinitionLoader, clock);
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
        when(permissionGroupService.getByCodeOrThrow("GROUP1")).thenReturn(group);

        // MenuDefinitionLoader mock setup
        MenuDefinition.CapabilityRef capRef = new MenuDefinition.CapabilityRef();
        capRef.setFeature("FILE");
        capRef.setAction("READ");
        MenuDefinition menuDef = new MenuDefinition();
        menuDef.setCode("FILE_MENU");
        menuDef.setName("파일 관리");
        menuDef.setPath("/file");
        menuDef.setRequiredCapabilities(List.of(capRef));
        menuDef.setChildren(List.of());
        MenuDefinitions definitions = new MenuDefinitions();
        definitions.setMenus(List.of(menuDef));
        when(menuDefinitionLoader.load()).thenReturn(definitions);

        PermissionMenuReadModel model = source.snapshot("user1");

        assertThat(model.items()).hasSize(1);
        assertThat(model.items().get(0).featureCode()).isEqualTo("FILE");
        assertThat(model.items().get(0).actionCode()).isEqualTo("READ");
        assertThat(model.items().get(0).path()).isEqualTo("/file");
    }

    @Test
    void fallsBackToDefaultGroupWhenAccountHasNoGroup() {
        UserAccount account = UserAccount.builder()
                .username("user2")
                .password("pw")
                .organizationCode("ORG2")
                .permissionGroupCode(null)
                .build();
        when(userAccountService.getByUsernameOrThrow("user2")).thenReturn(account);

        PermissionGroup group = new PermissionGroup("DEFAULT", "Default");
        group.replaceAssignments(List.of());
        when(permissionGroupService.getByCodeOrThrow("DEFAULT")).thenReturn(group);

        // Empty menu definitions
        MenuDefinitions definitions = new MenuDefinitions();
        definitions.setMenus(List.of());
        when(menuDefinitionLoader.load()).thenReturn(definitions);

        PermissionMenuReadModel model = source.snapshot("user2");

        assertThat(model.items()).isEmpty();
    }
}
