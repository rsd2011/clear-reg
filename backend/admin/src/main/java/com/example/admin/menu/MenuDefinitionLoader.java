package com.example.admin.menu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * YAML 파일에서 메뉴 정의를 로드하는 로더.
 */
@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "MenuDefinitions is loaded once at startup and cached")
@Component
public class MenuDefinitionLoader {

    private static final Logger log = LoggerFactory.getLogger(MenuDefinitionLoader.class);

    private final ObjectMapper yamlMapper;
    private final Resource menuDefinitionResource;
    private volatile MenuDefinitions cachedDefinitions;

    public MenuDefinitionLoader(
            @Value("${menu.definition.location:classpath:menu-definitions.yml}") Resource menuDefinitionResource) {
        this.menuDefinitionResource = menuDefinitionResource;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    /**
     * 메뉴 정의를 로드한다. 캐시가 있으면 캐시된 값을 반환한다.
     */
    public MenuDefinitions load() {
        if (cachedDefinitions != null) {
            return cachedDefinitions;
        }
        synchronized (this) {
            if (cachedDefinitions != null) {
                return cachedDefinitions;
            }
            cachedDefinitions = loadFromResource();
            return cachedDefinitions;
        }
    }

    /**
     * 캐시를 무효화하고 다시 로드한다.
     */
    public MenuDefinitions reload() {
        synchronized (this) {
            cachedDefinitions = null;
            return load();
        }
    }

    /**
     * 모든 메뉴 정의를 평탄화하여 반환한다 (부모-자식 관계 유지).
     */
    public List<MenuDefinition> loadFlattened() {
        MenuDefinitions definitions = load();
        return definitions.getMenus();
    }

    private MenuDefinitions loadFromResource() {
        if (!menuDefinitionResource.exists()) {
            log.warn("메뉴 정의 파일이 존재하지 않습니다: {}", menuDefinitionResource);
            return new MenuDefinitions();
        }

        try (InputStream is = menuDefinitionResource.getInputStream()) {
            MenuDefinitions definitions = yamlMapper.readValue(is, MenuDefinitions.class);
            log.info("메뉴 정의 로드 완료: {} 개의 최상위 메뉴", definitions.getMenus().size());
            return definitions;
        } catch (IOException e) {
            log.error("메뉴 정의 파일 로드 실패: {}", menuDefinitionResource, e);
            return new MenuDefinitions();
        }
    }
}
