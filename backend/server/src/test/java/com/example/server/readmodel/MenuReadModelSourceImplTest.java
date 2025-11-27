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

import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.PermissionAssignment;
import com.example.admin.permission.PermissionGroup;
import com.example.admin.permission.PermissionGroupRepository;
import com.example.dw.application.readmodel.MenuReadModel;

class MenuReadModelSourceImplTest {

    private final PermissionGroupRepository permissionGroupRepository = Mockito.mock(PermissionGroupRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2025-01-02T00:00:00Z"), ZoneOffset.UTC);

    private MenuReadModelSourceImpl source;

    @BeforeEach
    void setUp() {
        source = new MenuReadModelSourceImpl(permissionGroupRepository, clock);
    }

    @Test
    void buildsUniqueMenuItemsFromPermissionGroups() {
        PermissionGroup g1 = new PermissionGroup("G1", "Group1");
        g1.replaceAssignments(List.of(
                new PermissionAssignment(FeatureCode.FILE, ActionCode.READ, com.example.common.security.RowScope.ORG),
                new PermissionAssignment(FeatureCode.FILE, ActionCode.UNMASK, com.example.common.security.RowScope.ALL)
        ));
        when(permissionGroupRepository.findAll()).thenReturn(List.of(g1));

        MenuReadModel model = source.snapshot();

        assertThat(model.items()).hasSize(2);
        assertThat(model.items()).anyMatch(item -> item.featureCode().equals("FILE") && item.actionCode().equals("READ"));
        assertThat(model.items()).anyMatch(item -> item.featureCode().equals("FILE") && item.actionCode().equals("UNMASK"));
    }
}
