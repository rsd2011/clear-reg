package com.example.admin.menu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메뉴 가시성 서비스.
 *
 * <p>사용자에게 표시할 메뉴를 결정하는 서비스로, 두 단계의 평가를 수행한다:</p>
 * <ol>
 *   <li><b>1단계: Capability 기반 접근 제어</b> - 사용자가 메뉴의 requiredCapabilities 중
 *       하나라도 보유하면 기본적으로 접근 가능</li>
 *   <li><b>2단계: MenuViewConfig 기반 가시성 조정</b> - 역할/조직별로 추가적인
 *       표시/숨김/강조 설정 적용</li>
 * </ol>
 */
@Service
public class MenuVisibilityService {

    private static final Logger log = LoggerFactory.getLogger(MenuVisibilityService.class);

    private final MenuService menuService;
    private final MenuViewConfigRepository viewConfigRepository;

    public MenuVisibilityService(MenuService menuService,
                                  MenuViewConfigRepository viewConfigRepository) {
        this.menuService = menuService;
        this.viewConfigRepository = viewConfigRepository;
    }

    /**
     * 사용자에게 표시할 메뉴 목록을 결정한다.
     *
     * @param userCapabilities 사용자가 보유한 Capability 목록
     * @param permGroupCode 사용자의 권한 그룹 코드 (null 가능)
     * @param orgPolicyId 사용자의 조직 정책 ID (null 가능)
     * @return 가시성이 적용된 메뉴 정보 목록
     */
    @Transactional(readOnly = true)
    public List<VisibleMenu> determineVisibleMenus(Collection<MenuCapability> userCapabilities,
                                                    String permGroupCode,
                                                    Long orgPolicyId) {
        // 1단계: Capability 기반으로 접근 가능한 메뉴 조회
        List<Menu> accessibleMenus = menuService.findAccessibleMenus(userCapabilities);

        if (accessibleMenus.isEmpty()) {
            return List.of();
        }

        // 메뉴 코드 목록 추출
        List<String> menuCodes = accessibleMenus.stream()
                .map(Menu::getCode)
                .collect(Collectors.toList());

        // 2단계: 해당 메뉴들에 적용될 ViewConfig 조회
        List<MenuViewConfig> configs = viewConfigRepository.findApplicableConfigsForMenus(
                menuCodes, permGroupCode, orgPolicyId);

        // 메뉴 코드별 설정 매핑
        Map<String, List<MenuViewConfig>> configsByMenu = configs.stream()
                .collect(Collectors.groupingBy(MenuViewConfig::getMenuCode));

        // 가시성 적용
        List<VisibleMenu> result = new ArrayList<>();
        for (Menu menu : accessibleMenus) {
            List<MenuViewConfig> menuConfigs = configsByMenu.getOrDefault(
                    menu.getCode(), List.of());

            Optional<VisibleMenu> visibleMenu = applyVisibilityConfigs(menu, menuConfigs);
            visibleMenu.ifPresent(result::add);
        }

        return result;
    }

    /**
     * 특정 메뉴의 가시성을 결정한다.
     *
     * @param menuCode 메뉴 코드
     * @param userCapabilities 사용자가 보유한 Capability 목록
     * @param permGroupCode 사용자의 권한 그룹 코드
     * @param orgPolicyId 사용자의 조직 정책 ID
     * @return 가시성이 적용된 메뉴 정보 (접근 불가 또는 숨김이면 empty)
     */
    @Transactional(readOnly = true)
    public Optional<VisibleMenu> determineMenuVisibility(String menuCode,
                                                          Collection<MenuCapability> userCapabilities,
                                                          String permGroupCode,
                                                          Long orgPolicyId) {
        // 1단계: 메뉴 존재 여부 및 Capability 체크
        Optional<Menu> menuOpt = menuService.findActiveByCode(menuCode);
        if (menuOpt.isEmpty()) {
            return Optional.empty();
        }

        Menu menu = menuOpt.get();

        // Capability 체크
        Set<MenuCapability> capabilitySet = userCapabilities != null
                ? new HashSet<>(userCapabilities) : Set.of();

        if (!hasRequiredCapability(menu, capabilitySet)) {
            return Optional.empty();
        }

        // 2단계: ViewConfig 적용
        List<MenuViewConfig> configs = viewConfigRepository.findApplicableConfigs(
                menuCode, permGroupCode, orgPolicyId);

        return applyVisibilityConfigs(menu, configs);
    }

    /**
     * 메뉴가 요구하는 Capability 중 하나라도 사용자가 보유하는지 확인한다.
     */
    private boolean hasRequiredCapability(Menu menu, Set<MenuCapability> userCapabilities) {
        Set<MenuCapability> required = menu.getRequiredCapabilities();
        if (required.isEmpty()) {
            // 요구하는 Capability가 없으면 기본 표시
            return true;
        }
        for (MenuCapability cap : required) {
            if (userCapabilities.contains(cap)) {
                return true;
            }
        }
        return false;
    }

    /**
     * ViewConfig 설정을 적용하여 최종 가시성을 결정한다.
     */
    private Optional<VisibleMenu> applyVisibilityConfigs(Menu menu,
                                                          List<MenuViewConfig> configs) {
        // 기본값: SHOW
        MenuViewConfig.VisibilityAction finalAction = MenuViewConfig.VisibilityAction.SHOW;

        // 우선순위가 높은(낮은 숫자) 설정이 먼저 적용됨
        // configs는 이미 priority ASC로 정렬되어 있음
        if (!configs.isEmpty()) {
            MenuViewConfig highestPriority = configs.get(0);
            finalAction = highestPriority.getVisibilityAction();

            log.debug("메뉴 '{}' 에 ViewConfig 적용: {} (type={}, priority={})",
                    menu.getCode(), finalAction,
                    highestPriority.getTargetType(),
                    highestPriority.getPriority());
        }

        // HIDE 액션이면 메뉴 표시 안 함
        if (finalAction == MenuViewConfig.VisibilityAction.HIDE) {
            return Optional.empty();
        }

        return Optional.of(new VisibleMenu(
                menu.getCode(),
                menu.getName(),
                menu.getPath(),
                menu.getIcon(),
                menu.getSortOrder(),
                menu.getParentCode(),
                menu.getDescription(),
                finalAction == MenuViewConfig.VisibilityAction.HIGHLIGHT
        ));
    }

    /**
     * 접근 가능하면서 부모 계층까지 포함한 메뉴 트리를 구성한다.
     *
     * @param userCapabilities 사용자가 보유한 Capability 목록
     * @param permGroupCode 사용자의 권한 그룹 코드
     * @param orgPolicyId 사용자의 조직 정책 ID
     * @return 메뉴 트리 (루트 메뉴 목록)
     */
    @Transactional(readOnly = true)
    public List<MenuTreeNode> buildVisibleMenuTree(Collection<MenuCapability> userCapabilities,
                                                    String permGroupCode,
                                                    Long orgPolicyId) {
        List<VisibleMenu> visibleMenus = determineVisibleMenus(
                userCapabilities, permGroupCode, orgPolicyId);

        if (visibleMenus.isEmpty()) {
            return List.of();
        }

        // 가시적인 메뉴 코드 집합
        Set<String> visibleCodes = visibleMenus.stream()
                .map(VisibleMenu::code)
                .collect(Collectors.toSet());

        // 메뉴 맵 생성
        Map<String, VisibleMenu> menuMap = visibleMenus.stream()
                .collect(Collectors.toMap(VisibleMenu::code, m -> m));

        // 부모가 있는 경우 부모도 포함해야 함 (트리 구조 유지)
        Set<String> allRequiredCodes = new HashSet<>(visibleCodes);
        for (VisibleMenu vm : visibleMenus) {
            String parentCode = vm.parentCode();
            while (parentCode != null) {
                allRequiredCodes.add(parentCode);
                Optional<Menu> parent = menuService.findActiveByCode(parentCode);
                if (parent.isPresent()) {
                    parentCode = parent.get().getParentCode();
                } else {
                    break;
                }
            }
        }

        // 누락된 부모 메뉴 추가 (Capability 체크 없이 - 자식이 보이면 부모도 보여야 함)
        for (String code : allRequiredCodes) {
            if (!menuMap.containsKey(code)) {
                Optional<Menu> menu = menuService.findActiveByCode(code);
                menu.ifPresent(m -> menuMap.put(code, new VisibleMenu(
                        m.getCode(), m.getName(), m.getPath(), m.getIcon(),
                        m.getSortOrder(), m.getParentCode(), m.getDescription(), false)));
            }
        }

        // 트리 구성
        Map<String, MenuTreeNode> nodeMap = new HashMap<>();
        for (VisibleMenu vm : menuMap.values()) {
            nodeMap.put(vm.code(), new MenuTreeNode(vm, new ArrayList<>()));
        }

        List<MenuTreeNode> roots = new ArrayList<>();
        for (MenuTreeNode node : nodeMap.values()) {
            String parentCode = node.menu().parentCode();
            if (parentCode == null || !nodeMap.containsKey(parentCode)) {
                roots.add(node);
            } else {
                nodeMap.get(parentCode).children().add(node);
            }
        }

        // 자식 정렬
        sortChildren(roots);

        return roots;
    }

    private void sortChildren(List<MenuTreeNode> nodes) {
        nodes.sort((a, b) -> {
            Integer orderA = a.menu().sortOrder();
            Integer orderB = b.menu().sortOrder();
            if (orderA == null && orderB == null) return 0;
            if (orderA == null) return 1;
            if (orderB == null) return -1;
            return orderA.compareTo(orderB);
        });

        for (MenuTreeNode node : nodes) {
            if (!node.children().isEmpty()) {
                sortChildren(node.children());
            }
        }
    }

    /**
     * 가시적인 메뉴 정보.
     */
    public record VisibleMenu(
            String code,
            String name,
            String path,
            String icon,
            Integer sortOrder,
            String parentCode,
            String description,
            boolean highlighted
    ) {}

    /**
     * 메뉴 트리 노드.
     */
    public record MenuTreeNode(
            VisibleMenu menu,
            List<MenuTreeNode> children
    ) {}
}
