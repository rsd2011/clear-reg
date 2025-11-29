package com.example.admin.menu.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.example.admin.menu.domain.Menu;
import com.example.admin.menu.domain.MenuCapability;
import com.example.admin.menu.repository.MenuRepository;
import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메뉴 관리 서비스.
 *
 * <p>메뉴의 CRUD 및 Capability 기반 메뉴 조회를 제공한다.</p>
 *
 * <p>메뉴의 계층 구조와 가시성은 {@link MenuVisibilityService}에서 담당한다.</p>
 */
@Service
public class MenuService {

    private static final Logger log = LoggerFactory.getLogger(MenuService.class);

    private final MenuRepository menuRepository;

    public MenuService(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
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
    // 생성/수정/삭제
    // ========================================

    /**
     * 새 메뉴를 생성한다.
     */
    @Transactional
    public Menu createMenu(String code, String name, String path, String icon,
                           Integer sortOrder, String description,
                           Collection<MenuCapability> capabilities) {
        if (menuRepository.existsByCode(code)) {
            throw new IllegalArgumentException("이미 존재하는 메뉴 코드입니다: " + code);
        }

        Menu menu = new Menu(code, name);
        menu.updateDetails(name, path, icon, sortOrder, description);

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
}
