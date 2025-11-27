package com.example.admin.menu;

import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메뉴 관리 서비스.
 *
 * <p>메뉴의 CRUD 및 Capability 기반 메뉴 조회를 제공한다.</p>
 */
@Service
public class MenuService {

    private static final Logger log = LoggerFactory.getLogger(MenuService.class);

    private final MenuRepository menuRepository;
    private final MenuDefinitionLoader menuDefinitionLoader;

    public MenuService(MenuRepository menuRepository, MenuDefinitionLoader menuDefinitionLoader) {
        this.menuRepository = menuRepository;
        this.menuDefinitionLoader = menuDefinitionLoader;
    }

    // ========================================
    // 조회 메서드
    // ========================================

    /**
     * ID로 메뉴를 조회한다.
     */
    @Transactional(readOnly = true)
    public Optional<Menu> findById(UUID id) {
        return menuRepository.findById(id);
    }

    /**
     * 메뉴 코드로 조회한다.
     */
    @Transactional(readOnly = true)
    public Optional<Menu> findByCode(String code) {
        return menuRepository.findByCode(code);
    }

    /**
     * 활성화된 메뉴 코드로 조회한다.
     */
    @Transactional(readOnly = true)
    public Optional<Menu> findActiveByCode(String code) {
        return menuRepository.findByCodeAndActiveTrue(code);
    }

    /**
     * 모든 활성화된 메뉴를 정렬 순서대로 조회한다.
     */
    @Transactional(readOnly = true)
    public List<Menu> findAllActive() {
        return menuRepository.findByActiveTrueOrderBySortOrderAsc();
    }

    /**
     * 최상위 메뉴 목록을 조회한다 (부모가 없는 메뉴).
     */
    @Transactional(readOnly = true)
    public List<Menu> findRootMenus() {
        return menuRepository.findByParentIsNullAndActiveTrueOrderBySortOrderAsc();
    }

    /**
     * 특정 부모의 자식 메뉴 목록을 조회한다.
     */
    @Transactional(readOnly = true)
    public List<Menu> findChildrenByParentCode(String parentCode) {
        return menuRepository.findByParent_CodeAndActiveTrueOrderBySortOrderAsc(parentCode);
    }

    /**
     * 특정 Capability를 요구하는 메뉴 목록을 조회한다.
     */
    @Transactional(readOnly = true)
    public List<Menu> findByRequiredCapability(FeatureCode feature, ActionCode action) {
        return menuRepository.findByRequiredCapability(feature, action);
    }

    /**
     * 사용자가 보유한 Capability 목록을 기반으로 접근 가능한 메뉴 목록을 조회한다.
     *
     * <p>메뉴의 requiredCapabilities 중 하나라도 사용자가 보유하면 접근 가능하다.</p>
     *
     * @param userCapabilities 사용자가 보유한 Capability 목록
     * @return 접근 가능한 메뉴 목록
     */
    @Transactional(readOnly = true)
    public List<Menu> findAccessibleMenus(Collection<MenuCapability> userCapabilities) {
        if (userCapabilities == null || userCapabilities.isEmpty()) {
            return List.of();
        }

        Set<MenuCapability> capabilitySet = new HashSet<>(userCapabilities);
        List<Menu> allActiveMenus = findAllActive();

        return allActiveMenus.stream()
                .filter(menu -> hasAnyCapability(menu, capabilitySet))
                .collect(Collectors.toList());
    }

    /**
     * 메뉴가 요구하는 Capability 중 하나라도 사용자가 보유하는지 확인한다.
     */
    private boolean hasAnyCapability(Menu menu, Set<MenuCapability> userCapabilities) {
        Set<MenuCapability> required = menu.getRequiredCapabilities();
        if (required.isEmpty()) {
            // 요구하는 Capability가 없으면 모든 사용자에게 표시
            return true;
        }
        for (MenuCapability cap : required) {
            if (userCapabilities.contains(cap)) {
                return true;
            }
        }
        return false;
    }

    // ========================================
    // 메뉴 트리 구성
    // ========================================

    /**
     * 접근 가능한 메뉴를 트리 구조로 반환한다.
     *
     * @param userCapabilities 사용자가 보유한 Capability 목록
     * @return 루트 메뉴 목록 (자식 메뉴 포함)
     */
    @Transactional(readOnly = true)
    public List<Menu> buildAccessibleMenuTree(Collection<MenuCapability> userCapabilities) {
        List<Menu> accessibleMenus = findAccessibleMenus(userCapabilities);
        Set<String> accessibleCodes = accessibleMenus.stream()
                .map(Menu::getCode)
                .collect(Collectors.toSet());

        // 접근 가능한 자식이 있는 부모 메뉴도 포함
        Set<String> codesWithAccessibleChildren = new HashSet<>(accessibleCodes);
        for (Menu menu : accessibleMenus) {
            Menu parent = menu.getParent();
            while (parent != null) {
                codesWithAccessibleChildren.add(parent.getCode());
                parent = parent.getParent();
            }
        }

        // 루트 메뉴 중 접근 가능한 것만 필터링
        return findRootMenus().stream()
                .filter(menu -> codesWithAccessibleChildren.contains(menu.getCode()))
                .collect(Collectors.toList());
    }

    // ========================================
    // YAML 동기화
    // ========================================

    /**
     * YAML 정의 파일로부터 메뉴를 동기화한다.
     *
     * <p>YAML에 정의된 메뉴가 DB에 없으면 생성하고, 있으면 업데이트한다.</p>
     */
    @Transactional
    public void syncFromYaml() {
        List<MenuDefinition> definitions = menuDefinitionLoader.loadFlattened();
        log.info("YAML에서 {} 개의 메뉴 정의를 로드했습니다.", definitions.size());

        for (MenuDefinition def : definitions) {
            syncMenu(def, null);
        }

        log.info("메뉴 동기화 완료");
    }

    private void syncMenu(MenuDefinition def, Menu parent) {
        Optional<Menu> existing = menuRepository.findByCode(def.getCode());
        Menu menu;

        if (existing.isPresent()) {
            menu = existing.get();
            log.debug("기존 메뉴 업데이트: {}", def.getCode());
        } else {
            menu = new Menu(def.getCode(), def.getName());
            log.debug("새 메뉴 생성: {}", def.getCode());
        }

        // 상세 정보 업데이트
        menu.updateDetails(
                def.getName(),
                def.getPath(),
                def.getIcon(),
                def.getSortOrder(),
                def.getDescription()
        );
        menu.setParent(parent);
        menu.setActive(true);

        // Capability 업데이트
        Set<MenuCapability> capabilities = new HashSet<>();
        if (def.getRequiredCapabilities() != null) {
            for (MenuDefinition.CapabilityRef capRef : def.getRequiredCapabilities()) {
                try {
                    FeatureCode feature = FeatureCode.valueOf(capRef.getFeature());
                    ActionCode action = ActionCode.valueOf(capRef.getAction());
                    capabilities.add(new MenuCapability(feature, action));
                } catch (IllegalArgumentException e) {
                    log.warn("유효하지 않은 Capability: {}/{} - 메뉴: {}",
                            capRef.getFeature(), capRef.getAction(), def.getCode());
                }
            }
        }
        menu.replaceCapabilities(capabilities);

        menuRepository.save(menu);

        // 자식 메뉴 동기화
        if (def.getChildren() != null) {
            for (MenuDefinition child : def.getChildren()) {
                syncMenu(child, menu);
            }
        }
    }

    // ========================================
    // 생성/수정/삭제
    // ========================================

    /**
     * 새 메뉴를 생성한다.
     */
    @Transactional
    public Menu createMenu(String code, String name, String path, String icon,
                           Integer sortOrder, String description, String parentCode,
                           Collection<MenuCapability> capabilities) {
        if (menuRepository.existsByCode(code)) {
            throw new IllegalArgumentException("이미 존재하는 메뉴 코드입니다: " + code);
        }

        Menu menu = new Menu(code, name);
        menu.updateDetails(name, path, icon, sortOrder, description);

        if (parentCode != null) {
            Menu parent = menuRepository.findByCode(parentCode)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "부모 메뉴를 찾을 수 없습니다: " + parentCode));
            menu.setParent(parent);
        }

        if (capabilities != null) {
            menu.replaceCapabilities(capabilities);
        }

        return menuRepository.save(menu);
    }

    /**
     * 메뉴 정보를 수정한다.
     */
    @Transactional
    public Menu updateMenu(String code, String name, String path, String icon,
                           Integer sortOrder, String description,
                           Collection<MenuCapability> capabilities) {
        Menu menu = menuRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException(
                        "메뉴를 찾을 수 없습니다: " + code));

        menu.updateDetails(name, path, icon, sortOrder, description);

        if (capabilities != null) {
            menu.replaceCapabilities(capabilities);
        }

        return menuRepository.save(menu);
    }

    /**
     * 메뉴를 비활성화한다 (소프트 삭제).
     */
    @Transactional
    public void deactivateMenu(String code) {
        Menu menu = menuRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException(
                        "메뉴를 찾을 수 없습니다: " + code));
        menu.setActive(false);
        menuRepository.save(menu);
        log.info("메뉴 비활성화: {}", code);
    }

    /**
     * 메뉴를 활성화한다.
     */
    @Transactional
    public void activateMenu(String code) {
        Menu menu = menuRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException(
                        "메뉴를 찾을 수 없습니다: " + code));
        menu.setActive(true);
        menuRepository.save(menu);
        log.info("메뉴 활성화: {}", code);
    }

    /**
     * 메뉴의 부모를 변경한다.
     */
    @Transactional
    public void moveMenu(String code, String newParentCode) {
        Menu menu = menuRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException(
                        "메뉴를 찾을 수 없습니다: " + code));

        if (newParentCode == null) {
            menu.setParent(null);
        } else {
            if (newParentCode.equals(code)) {
                throw new IllegalArgumentException("메뉴를 자기 자신의 하위로 이동할 수 없습니다.");
            }
            Menu newParent = menuRepository.findByCode(newParentCode)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "부모 메뉴를 찾을 수 없습니다: " + newParentCode));

            // 순환 참조 체크
            if (isDescendant(newParent, menu)) {
                throw new IllegalArgumentException("순환 참조가 발생합니다: " + code + " -> " + newParentCode);
            }

            menu.setParent(newParent);
        }

        menuRepository.save(menu);
        log.info("메뉴 이동: {} -> {}", code, newParentCode);
    }

    /**
     * target이 ancestor의 하위인지 확인한다.
     */
    private boolean isDescendant(Menu target, Menu ancestor) {
        Menu current = target.getParent();
        while (current != null) {
            if (current.getCode().equals(ancestor.getCode())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
}
