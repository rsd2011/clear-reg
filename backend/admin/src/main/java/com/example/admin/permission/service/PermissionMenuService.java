package com.example.admin.permission.service;

import com.example.admin.menu.domain.Menu;
import com.example.admin.menu.domain.MenuCapability;
import com.example.admin.menu.repository.MenuRepository;
import com.example.admin.permission.domain.PermissionMenu;
import com.example.admin.permission.repository.PermissionMenuRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 권한 그룹별 메뉴 서비스.
 *
 * <p>권한 그룹에 따른 메뉴 트리 구성 및 관리를 담당한다.
 * 메뉴 트리는 권한 그룹별로 캐시된다.</p>
 */
@Service
public class PermissionMenuService {

    private static final Logger log = LoggerFactory.getLogger(PermissionMenuService.class);
    public static final String CACHE_NAME = "permissionMenus";

    private final PermissionMenuRepository permissionMenuRepository;
    private final MenuRepository menuRepository;

    public PermissionMenuService(PermissionMenuRepository permissionMenuRepository,
                                  MenuRepository menuRepository) {
        this.permissionMenuRepository = permissionMenuRepository;
        this.menuRepository = menuRepository;
    }

    // ========== Query Methods ==========

    /**
     * 권한 그룹의 전체 메뉴 트리를 조회한다.
     *
     * <p>캐시되며, 해당 권한 그룹의 메뉴가 변경되면 캐시가 무효화된다.</p>
     *
     * @param permGroupCode 권한 그룹 코드
     * @return 메뉴 트리 (루트 노드 목록)
     */
    @Cacheable(value = CACHE_NAME, key = "#permGroupCode")
    @Transactional(readOnly = true)
    public List<MenuTreeNode> getMenuTree(String permGroupCode) {
        Objects.requireNonNull(permGroupCode, "permGroupCode must not be null");

        List<PermissionMenu> allMenus = permissionMenuRepository
                .findByPermissionGroupCode(permGroupCode);

        if (allMenus.isEmpty()) {
            return List.of();
        }

        return buildTree(allMenus);
    }

    /**
     * 사용자 Capability를 고려하여 접근 가능한 메뉴 트리를 조회한다.
     *
     * @param permGroupCode 권한 그룹 코드
     * @param userCapabilities 사용자 보유 Capability
     * @return 접근 가능한 메뉴 트리
     */
    @Transactional(readOnly = true)
    public List<MenuTreeNode> getAccessibleMenuTree(String permGroupCode,
                                                     Collection<MenuCapability> userCapabilities) {
        List<MenuTreeNode> fullTree = getMenuTree(permGroupCode);

        if (userCapabilities == null || userCapabilities.isEmpty()) {
            // Capability가 없으면 카테고리만 보이고 메뉴는 안 보임
            return filterAccessibleNodes(fullTree, Set.of());
        }

        Set<MenuCapability> capSet = userCapabilities instanceof Set
                ? (Set<MenuCapability>) userCapabilities
                : Set.copyOf(userCapabilities);

        return filterAccessibleNodes(fullTree, capSet);
    }

    /**
     * 권한 그룹에서 특정 메뉴를 조회한다.
     */
    @Transactional(readOnly = true)
    public Optional<PermissionMenu> findByMenuCode(String permGroupCode, String menuCode) {
        return permissionMenuRepository.findByPermissionGroupCodeAndMenuCode(
                permGroupCode, menuCode);
    }

    /**
     * 권한 그룹에서 특정 카테고리를 조회한다.
     */
    @Transactional(readOnly = true)
    public Optional<PermissionMenu> findByCategoryCode(String permGroupCode, String categoryCode) {
        return permissionMenuRepository.findByPermissionGroupCodeAndCategoryCode(
                permGroupCode, categoryCode);
    }

    // ========== Command Methods ==========

    /**
     * 메뉴를 권한 그룹에 추가한다.
     */
    @CacheEvict(value = CACHE_NAME, key = "#permGroupCode")
    @Transactional
    public PermissionMenu addMenu(String permGroupCode, String menuCode,
                                   UUID parentId, Integer displayOrder) {
        Objects.requireNonNull(permGroupCode, "permGroupCode must not be null");
        Objects.requireNonNull(menuCode, "menuCode must not be null");

        Menu menu = menuRepository.findByCode(menuCode)
                .orElseThrow(() -> new IllegalArgumentException("Menu not found: " + menuCode));

        PermissionMenu parent = parentId != null
                ? permissionMenuRepository.findById(parentId).orElse(null)
                : null;

        PermissionMenu pm = PermissionMenu.forMenu(permGroupCode, menu, parent, displayOrder);
        PermissionMenu saved = permissionMenuRepository.save(pm);

        log.info("Added menu '{}' to permission group '{}'", menuCode, permGroupCode);
        return saved;
    }

    /**
     * 카테고리를 권한 그룹에 추가한다.
     */
    @CacheEvict(value = CACHE_NAME, key = "#permGroupCode")
    @Transactional
    public PermissionMenu addCategory(String permGroupCode,
                                       String categoryCode, String categoryName, String categoryIcon,
                                       UUID parentId, Integer displayOrder) {
        Objects.requireNonNull(permGroupCode, "permGroupCode must not be null");
        Objects.requireNonNull(categoryCode, "categoryCode must not be null");
        Objects.requireNonNull(categoryName, "categoryName must not be null");

        PermissionMenu parent = parentId != null
                ? permissionMenuRepository.findById(parentId).orElse(null)
                : null;

        PermissionMenu pm = PermissionMenu.forCategory(
                permGroupCode, categoryCode, categoryName, categoryIcon, parent, displayOrder);
        PermissionMenu saved = permissionMenuRepository.save(pm);

        log.info("Added category '{}' to permission group '{}'", categoryCode, permGroupCode);
        return saved;
    }

    /**
     * PermissionMenu를 삭제한다.
     */
    @CacheEvict(value = CACHE_NAME, key = "#permGroupCode")
    @Transactional
    public void remove(String permGroupCode, UUID permissionMenuId) {
        permissionMenuRepository.deleteById(permissionMenuId);
        log.info("Removed permission menu {} from group '{}'", permissionMenuId, permGroupCode);
    }

    /**
     * 권한 그룹의 모든 메뉴 설정을 삭제한다.
     */
    @CacheEvict(value = CACHE_NAME, key = "#permGroupCode")
    @Transactional
    public void removeAllByPermissionGroup(String permGroupCode) {
        permissionMenuRepository.deleteByPermissionGroupCode(permGroupCode);
        log.info("Removed all permission menus from group '{}'", permGroupCode);
    }

    /**
     * 표시 순서를 변경한다.
     */
    @CacheEvict(value = CACHE_NAME, key = "#permGroupCode")
    @Transactional
    public void updateDisplayOrder(String permGroupCode, UUID permissionMenuId, Integer displayOrder) {
        PermissionMenu pm = permissionMenuRepository.findById(permissionMenuId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "PermissionMenu not found: " + permissionMenuId));

        pm.setDisplayOrder(displayOrder);
        log.debug("Updated display order of {} to {}", permissionMenuId, displayOrder);
    }

    /**
     * 부모를 변경한다.
     */
    @CacheEvict(value = CACHE_NAME, key = "#permGroupCode")
    @Transactional
    public void updateParent(String permGroupCode, UUID permissionMenuId, UUID newParentId) {
        PermissionMenu pm = permissionMenuRepository.findById(permissionMenuId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "PermissionMenu not found: " + permissionMenuId));

        PermissionMenu newParent = newParentId != null
                ? permissionMenuRepository.findById(newParentId).orElse(null)
                : null;

        pm.setParent(newParent);
        log.debug("Updated parent of {} to {}", permissionMenuId, newParentId);
    }

    // ========== Private Helpers ==========

    private List<MenuTreeNode> buildTree(List<PermissionMenu> allMenus) {
        Map<UUID, PermissionMenu> menuMap = allMenus.stream()
                .collect(Collectors.toMap(PermissionMenu::getId, pm -> pm));

        Map<UUID, List<PermissionMenu>> childrenMap = new HashMap<>();
        List<PermissionMenu> roots = new ArrayList<>();

        for (PermissionMenu pm : allMenus) {
            if (pm.getParent() == null) {
                roots.add(pm);
            } else {
                childrenMap.computeIfAbsent(pm.getParent().getId(), k -> new ArrayList<>())
                        .add(pm);
            }
        }

        return roots.stream()
                .map(pm -> buildNode(pm, childrenMap))
                .sorted(this::compareByDisplayOrder)
                .toList();
    }

    private MenuTreeNode buildNode(PermissionMenu pm, Map<UUID, List<PermissionMenu>> childrenMap) {
        List<MenuTreeNode> children = childrenMap.getOrDefault(pm.getId(), List.of())
                .stream()
                .map(child -> buildNode(child, childrenMap))
                .sorted(this::compareByDisplayOrder)
                .toList();

        return new MenuTreeNode(
                pm.getId(),
                pm.getCode(),
                pm.getName(),
                pm.getPath(),
                pm.getIcon(),
                pm.getDisplayOrder(),
                pm.isCategory(),
                pm.isMenu() ? pm.getMenu().getRequiredCapabilities() : Set.of(),
                children
        );
    }

    private int compareByDisplayOrder(MenuTreeNode a, MenuTreeNode b) {
        if (a.displayOrder() == null && b.displayOrder() == null) return 0;
        if (a.displayOrder() == null) return 1;
        if (b.displayOrder() == null) return -1;
        return a.displayOrder().compareTo(b.displayOrder());
    }

    private List<MenuTreeNode> filterAccessibleNodes(List<MenuTreeNode> nodes,
                                                      Set<MenuCapability> userCaps) {
        List<MenuTreeNode> result = new ArrayList<>();

        for (MenuTreeNode node : nodes) {
            List<MenuTreeNode> filteredChildren = filterAccessibleNodes(node.children(), userCaps);

            if (node.isCategory()) {
                // 카테고리는 자식이 하나라도 있으면 표시
                if (!filteredChildren.isEmpty()) {
                    result.add(node.withChildren(filteredChildren));
                }
            } else {
                // 메뉴는 Capability 체크
                if (isAccessible(node, userCaps)) {
                    result.add(node.withChildren(filteredChildren));
                }
            }
        }

        return result;
    }

    private boolean isAccessible(MenuTreeNode node, Set<MenuCapability> userCaps) {
        if (node.requiredCapabilities().isEmpty()) {
            // 요구 Capability가 없으면 누구나 접근 가능
            return true;
        }
        // 하나라도 보유하면 접근 가능
        for (MenuCapability cap : node.requiredCapabilities()) {
            if (userCaps.contains(cap)) {
                return true;
            }
        }
        return false;
    }

    // ========== Record Types ==========

    /**
     * 메뉴 트리 노드.
     */
    public record MenuTreeNode(
            UUID id,
            String code,
            String name,
            String path,
            String icon,
            Integer displayOrder,
            boolean isCategory,
            Set<MenuCapability> requiredCapabilities,
            List<MenuTreeNode> children
    ) {
        public MenuTreeNode withChildren(List<MenuTreeNode> newChildren) {
            return new MenuTreeNode(
                    id, code, name, path, icon, displayOrder,
                    isCategory, requiredCapabilities, newChildren
            );
        }
    }
}
