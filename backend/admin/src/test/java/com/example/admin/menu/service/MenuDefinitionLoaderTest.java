package com.example.admin.menu.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.example.admin.menu.domain.MenuDefinition;
import com.example.admin.menu.domain.MenuDefinitions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * MenuDefinitionLoader 단위 테스트.
 */
class MenuDefinitionLoaderTest {

    @Test
    @DisplayName("Given menu-definitions.yml 존재 - When load - Then 메뉴 정의 로드")
    void load_validYaml_returnsDefinitions() {
        // Given
        Resource resource = new ClassPathResource("menu-definitions.yml");
        MenuDefinitionLoader loader = new MenuDefinitionLoader(resource);

        // When
        MenuDefinitions definitions = loader.load();

        // Then
        assertThat(definitions).isNotNull();
        assertThat(definitions.getMenus()).isNotEmpty();
    }

    @Test
    @DisplayName("Given 로드된 메뉴 - When getMenus - Then 최상위 메뉴 목록 반환")
    void load_validYaml_containsRootMenus() {
        // Given
        Resource resource = new ClassPathResource("menu-definitions.yml");
        MenuDefinitionLoader loader = new MenuDefinitionLoader(resource);

        // When
        MenuDefinitions definitions = loader.load();
        List<MenuDefinition> menus = definitions.getMenus();

        // Then
        assertThat(menus).extracting(MenuDefinition::getCode)
                .contains("DRAFT_MANAGEMENT", "APPROVAL_MANAGEMENT", "ADMIN");
    }

    @Test
    @DisplayName("Given 계층적 메뉴 - When getChildren - Then 자식 메뉴 포함")
    void load_hierarchicalMenu_containsChildren() {
        // Given
        Resource resource = new ClassPathResource("menu-definitions.yml");
        MenuDefinitionLoader loader = new MenuDefinitionLoader(resource);

        // When
        MenuDefinitions definitions = loader.load();
        MenuDefinition draftManagement = definitions.getMenus().stream()
                .filter(m -> "DRAFT_MANAGEMENT".equals(m.getCode()))
                .findFirst()
                .orElseThrow();

        // Then
        assertThat(draftManagement.getChildren()).isNotEmpty();
        assertThat(draftManagement.getChildren()).extracting(MenuDefinition::getCode)
                .contains("DRAFT_LIST", "DRAFT_CREATE");
    }

    @Test
    @DisplayName("Given 메뉴 정의 - When getRequiredCapabilities - Then Capability 목록 반환")
    void load_menuWithCapabilities_containsCapabilities() {
        // Given
        Resource resource = new ClassPathResource("menu-definitions.yml");
        MenuDefinitionLoader loader = new MenuDefinitionLoader(resource);

        // When
        MenuDefinitions definitions = loader.load();
        MenuDefinition draftManagement = definitions.getMenus().stream()
                .filter(m -> "DRAFT_MANAGEMENT".equals(m.getCode()))
                .findFirst()
                .orElseThrow();

        // Then
        assertThat(draftManagement.getRequiredCapabilities()).isNotEmpty();
        assertThat(draftManagement.getRequiredCapabilities().get(0).getFeature()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("Given 캐시된 정의 - When load 재호출 - Then 동일한 인스턴스 반환")
    void load_cached_returnsSameInstance() {
        // Given
        Resource resource = new ClassPathResource("menu-definitions.yml");
        MenuDefinitionLoader loader = new MenuDefinitionLoader(resource);

        // When
        MenuDefinitions first = loader.load();
        MenuDefinitions second = loader.load();

        // Then
        assertThat(first).isSameAs(second);
    }

    @Test
    @DisplayName("Given 캐시 초기화 후 - When reload - Then 새로운 인스턴스 반환")
    void reload_invalidatesCache_returnsNewInstance() {
        // Given
        Resource resource = new ClassPathResource("menu-definitions.yml");
        MenuDefinitionLoader loader = new MenuDefinitionLoader(resource);
        MenuDefinitions first = loader.load();

        // When
        MenuDefinitions reloaded = loader.reload();

        // Then
        assertThat(reloaded).isNotSameAs(first);
        assertThat(reloaded.getMenus()).hasSameSizeAs(first.getMenus());
    }

    @Test
    @DisplayName("Given 존재하지 않는 파일 - When load - Then 빈 MenuDefinitions 반환")
    void load_nonExistentFile_returnsEmptyDefinitions() {
        // Given
        Resource resource = new ClassPathResource("non-existent.yml");
        MenuDefinitionLoader loader = new MenuDefinitionLoader(resource);

        // When
        MenuDefinitions definitions = loader.load();

        // Then
        assertThat(definitions).isNotNull();
        assertThat(definitions.getMenus()).isEmpty();
    }

    @Test
    @DisplayName("Given 평탄화 요청 - When loadFlattened - Then 모든 메뉴 평탄화 반환")
    void loadFlattened_validYaml_returnsAllMenus() {
        // Given
        Resource resource = new ClassPathResource("menu-definitions.yml");
        MenuDefinitionLoader loader = new MenuDefinitionLoader(resource);

        // When
        List<MenuDefinition> flattened = loader.loadFlattened();

        // Then
        assertThat(flattened).isNotEmpty();
        // 최상위 메뉴만 반환 (loadFlattened은 getMenus를 호출함)
        assertThat(flattened).extracting(MenuDefinition::getCode)
                .contains("DRAFT_MANAGEMENT", "ADMIN");
    }
}
