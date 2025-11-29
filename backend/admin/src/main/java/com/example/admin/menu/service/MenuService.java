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
import com.example.admin.menu.domain.MenuCode;
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
    public Optional<Menu> findByCode(MenuCode code) {
        return menuRepository.findByCode(code);
    }

    /**
     * 활성화된 메뉴 코드로 조회한다.
     */
    @Transactional(readOnly = true)
    public Optional<Menu> findActiveByCode(MenuCode code) {
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
     * 메뉴 생성 (또는 기존 메뉴 업데이트).
     *
     * <p>MenuCode enum에 정의된 메뉴만 생성 가능하다.</p>
     *
     * @param code 메뉴 코드 (enum)
     * @param name 메뉴 이름
     * @param icon 아이콘 (null이면 enum 기본값 사용)
     * @param sortOrder 정렬 순서
     * @param description 설명
     * @param capabilities 접근 권한 목록
     * @return 생성 또는 수정된 메뉴
     */
    @Transactional
    public Menu createOrUpdateMenu(MenuCode code, String name, String icon,
                                    Integer sortOrder, String description,
                                    Collection<MenuCapability> capabilities) {
        Menu menu = menuRepository.findByCode(code)
                .orElseGet(() -> new Menu(code, name));

        menu.updateDetails(name, icon, sortOrder, description);

        if (capabilities != null) {
            menu.replaceCapabilities(capabilities);
        }

        return menuRepository.save(menu);
    }

    /**
     * 메뉴를 비활성화한다 (소프트 삭제).
     */
    @Transactional
    public void deactivateMenu(MenuCode code) {
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
    public void activateMenu(MenuCode code) {
        Menu menu = menuRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException(
                        "메뉴를 찾을 수 없습니다: " + code));
        menu.setActive(true);
        menuRepository.save(menu);
        log.info("메뉴 활성화: {}", code);
    }

    // ========================================
    // 초기화
    // ========================================

    /**
     * MenuCode enum에 정의된 모든 메뉴를 DB에 동기화.
     *
     * <p>애플리케이션 시작 시 또는 관리자 요청 시 실행한다.</p>
     * <p>이미 존재하는 메뉴는 건너뛴다.</p>
     *
     * @return 새로 생성된 메뉴 수
     */
    @Transactional
    public int syncMenusFromEnum() {
        int created = 0;
        for (MenuCode code : MenuCode.values()) {
            if (!menuRepository.existsByCode(code)) {
                Menu menu = new Menu(code, code.name());
                menuRepository.save(menu);
                log.info("메뉴 자동 생성: {}", code);
                created++;
            }
        }
        if (created > 0) {
            log.info("총 {}개 메뉴 동기화 완료", created);
        }
        return created;
    }
}
