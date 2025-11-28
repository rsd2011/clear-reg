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
import com.example.dw.application.readmodel.MenuReadModel;

class MenuReadModelSourceImplTest {

    private final MenuDefinitionLoader menuDefinitionLoader = Mockito.mock(MenuDefinitionLoader.class);
    private final Clock clock = Clock.fixed(Instant.parse("2025-01-02T00:00:00Z"), ZoneOffset.UTC);

    private MenuReadModelSourceImpl source;

    @BeforeEach
    void setUp() {
        source = new MenuReadModelSourceImpl(menuDefinitionLoader, clock);
    }

    @Test
    void buildsUniqueMenuItemsFromMenuDefinitions() {
        // Setup menu definitions with multiple capabilities
        MenuDefinition.CapabilityRef readCap = new MenuDefinition.CapabilityRef();
        readCap.setFeature("FILE");
        readCap.setAction("READ");

        MenuDefinition.CapabilityRef unmaskCap = new MenuDefinition.CapabilityRef();
        unmaskCap.setFeature("FILE");
        unmaskCap.setAction("UNMASK");

        MenuDefinition menuDef1 = new MenuDefinition();
        menuDef1.setCode("FILE_READ_MENU");
        menuDef1.setName("파일 읽기");
        menuDef1.setPath("/file/read");
        menuDef1.setRequiredCapabilities(List.of(readCap));
        menuDef1.setChildren(List.of());

        MenuDefinition menuDef2 = new MenuDefinition();
        menuDef2.setCode("FILE_UNMASK_MENU");
        menuDef2.setName("파일 언마스킹");
        menuDef2.setPath("/file/unmask");
        menuDef2.setRequiredCapabilities(List.of(unmaskCap));
        menuDef2.setChildren(List.of());

        MenuDefinitions definitions = new MenuDefinitions();
        definitions.setMenus(List.of(menuDef1, menuDef2));
        when(menuDefinitionLoader.load()).thenReturn(definitions);

        MenuReadModel model = source.snapshot();

        assertThat(model.items()).hasSize(2);
        assertThat(model.items()).anyMatch(item -> "FILE".equals(item.featureCode()) && "READ".equals(item.actionCode()));
        assertThat(model.items()).anyMatch(item -> "FILE".equals(item.featureCode()) && "UNMASK".equals(item.actionCode()));
    }
}
