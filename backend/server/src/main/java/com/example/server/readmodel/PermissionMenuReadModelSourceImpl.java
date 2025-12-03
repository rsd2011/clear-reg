package com.example.server.readmodel;

import com.example.admin.menu.domain.MenuCapability;
import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.admin.permission.domain.PermissionGroup;
import com.example.admin.permission.service.PermissionGroupService;
import com.example.admin.permission.service.PermissionMenuService;
import com.example.admin.permission.service.PermissionMenuService.MenuTreeNode;
import com.example.common.user.spi.UserAccountInfo;
import com.example.common.user.spi.UserAccountProvider;
import com.example.dw.application.readmodel.MenuItem.MenuCapabilityRef;
import com.example.dw.application.readmodel.PermissionMenuItem;
import com.example.dw.application.readmodel.PermissionMenuReadModel;
import com.example.dw.application.readmodel.PermissionMenuReadModelSource;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 사용자 권한 기반 메뉴 Read Model 생성기.
 *
 * <p>권한 그룹별 메뉴 트리에서 사용자의 권한으로 필터링하여 반환한다.
 * 메뉴의 requiredCapabilities 중 하나라도 사용자가 보유하면 메뉴가 포함된다.</p>
 */
@Component
public class PermissionMenuReadModelSourceImpl implements PermissionMenuReadModelSource {

    private static final String DEFAULT_PERMISSION_GROUP = "DEFAULT";

    private final UserAccountProvider userAccountProvider;
    private final PermissionGroupService permissionGroupService;
    private final PermissionMenuService permissionMenuService;
    private final Clock clock;

    public PermissionMenuReadModelSourceImpl(UserAccountProvider userAccountProvider,
                                             PermissionGroupService permissionGroupService,
                                             PermissionMenuService permissionMenuService,
                                             Clock clock) {
        this.userAccountProvider = userAccountProvider;
        this.permissionGroupService = permissionGroupService;
        this.permissionMenuService = permissionMenuService;
        this.clock = clock;
    }

    @Override
    public PermissionMenuReadModel snapshot(String principalId) {
        UserAccountInfo account = userAccountProvider.getByUsernameOrThrow(principalId);
        String groupCode = account.getPermissionGroupCode();
        if (groupCode == null) {
            groupCode = DEFAULT_PERMISSION_GROUP;
        }
        PermissionGroup group = permissionGroupService.getByCodeOrThrow(groupCode);

        List<MenuTreeNode> menuTree = permissionMenuService.getMenuTree(groupCode);
        List<PermissionMenuItem> items = new ArrayList<>();

        for (MenuTreeNode menu : menuTree) {
            filterAndAddMenu(menu, null, group, items);
        }

        // sortOrder 기준 정렬
        items.sort(Comparator.comparing(
                item -> item.sortOrder() != null ? item.sortOrder() : Integer.MAX_VALUE));

        return new PermissionMenuReadModel(
                UUID.randomUUID().toString(),
                OffsetDateTime.now(clock),
                items);
    }

    /**
     * 메뉴를 사용자 권한으로 필터링하여 추가한다.
     */
    private void filterAndAddMenu(MenuTreeNode menu, String parentCode, PermissionGroup group,
                                   List<PermissionMenuItem> items) {
        // 카테고리인 경우 자식 메뉴만 처리
        if (menu.isCategory()) {
            for (MenuTreeNode child : menu.children()) {
                filterAndAddMenu(child, menu.code(), group, items);
            }
            return;
        }

        // 사용자가 보유한 capability 중 메뉴에서 요구하는 capability와 일치하는 것 찾기
        List<MenuCapabilityRef> grantedCapabilities = findGrantedCapabilities(menu.requiredCapabilities(), group);

        // 하나라도 일치하면 메뉴 표시
        if (!grantedCapabilities.isEmpty()) {
            // 첫 번째 granted capability를 기본 featureCode/actionCode로 사용
            String featureCode = grantedCapabilities.get(0).feature();
            String actionCode = grantedCapabilities.get(0).action();

            PermissionMenuItem item = new PermissionMenuItem(
                    menu.code(),
                    menu.name(),
                    featureCode,
                    actionCode,
                    menu.path(),
                    menu.icon(),
                    menu.displayOrder(),
                    parentCode,
                    null,  // description은 MenuTreeNode에 없음
                    grantedCapabilities);

            items.add(item);

            // 자식 메뉴 재귀 처리 (부모가 표시되는 경우에만)
            for (MenuTreeNode child : menu.children()) {
                filterAndAddMenu(child, menu.code(), group, items);
            }
        }
    }

    /**
     * 사용자가 보유한 capability 중 메뉴에서 요구하는 것과 일치하는 capability 목록을 반환한다.
     */
    private List<MenuCapabilityRef> findGrantedCapabilities(Set<MenuCapability> requiredCapabilities,
                                                             PermissionGroup group) {
        List<MenuCapabilityRef> granted = new ArrayList<>();

        for (MenuCapability required : requiredCapabilities) {
            try {
                FeatureCode feature = required.getFeature();
                ActionCode action = required.getAction();

                if (group.assignmentFor(feature, action).isPresent()) {
                    granted.add(new MenuCapabilityRef(feature.name(), action.name()));
                }
            } catch (IllegalArgumentException e) {
                // 잘못된 FeatureCode/ActionCode는 무시
            }
        }

        return granted;
    }
}
