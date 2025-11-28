package com.example.server.readmodel;

import com.example.admin.menu.domain.MenuDefinition;
import com.example.admin.menu.service.MenuDefinitionLoader;
import com.example.admin.menu.domain.MenuDefinitions;
import com.example.admin.organization.OrganizationPolicyService;
import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.admin.permission.domain.PermissionGroup;
import com.example.admin.permission.service.PermissionGroupService;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountService;
import com.example.dw.application.readmodel.MenuItem.MenuCapabilityRef;
import com.example.dw.application.readmodel.PermissionMenuItem;
import com.example.dw.application.readmodel.PermissionMenuReadModel;
import com.example.dw.application.readmodel.PermissionMenuReadModelSource;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 사용자 권한 기반 메뉴 Read Model 생성기.
 *
 * <p>YAML에서 정의된 메뉴 목록을 사용자의 권한으로 필터링하여 반환한다.
 * 메뉴의 requiredCapabilities 중 하나라도 사용자가 보유하면 메뉴가 포함된다.</p>
 */
@Component
public class PermissionMenuReadModelSourceImpl implements PermissionMenuReadModelSource {

    private final UserAccountService userAccountService;
    private final PermissionGroupService permissionGroupService;
    private final OrganizationPolicyService organizationPolicyService;
    private final MenuDefinitionLoader menuDefinitionLoader;
    private final Clock clock;

    public PermissionMenuReadModelSourceImpl(UserAccountService userAccountService,
                                             PermissionGroupService permissionGroupService,
                                             OrganizationPolicyService organizationPolicyService,
                                             MenuDefinitionLoader menuDefinitionLoader,
                                             Clock clock) {
        this.userAccountService = userAccountService;
        this.permissionGroupService = permissionGroupService;
        this.organizationPolicyService = organizationPolicyService;
        this.menuDefinitionLoader = menuDefinitionLoader;
        this.clock = clock;
    }

    @Override
    public PermissionMenuReadModel snapshot(String principalId) {
        UserAccount account = userAccountService.getByUsernameOrThrow(principalId);
        String groupCode = account.getPermissionGroupCode();
        if (groupCode == null) {
            groupCode = organizationPolicyService.defaultPermissionGroup(account.getOrganizationCode());
        }
        PermissionGroup group = permissionGroupService.getByCodeOrThrow(groupCode);

        MenuDefinitions definitions = menuDefinitionLoader.load();
        List<PermissionMenuItem> items = new ArrayList<>();

        for (MenuDefinition menu : definitions.getMenus()) {
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
    private void filterAndAddMenu(MenuDefinition menu, String parentCode, PermissionGroup group,
                                   List<PermissionMenuItem> items) {
        // 사용자가 보유한 capability 중 메뉴에서 요구하는 capability와 일치하는 것 찾기
        List<MenuCapabilityRef> grantedCapabilities = findGrantedCapabilities(menu, group);

        // 하나라도 일치하면 메뉴 표시
        if (!grantedCapabilities.isEmpty()) {
            // 첫 번째 granted capability를 기본 featureCode/actionCode로 사용
            String featureCode = grantedCapabilities.get(0).feature();
            String actionCode = grantedCapabilities.get(0).action();

            PermissionMenuItem item = new PermissionMenuItem(
                    menu.getCode(),
                    menu.getName(),
                    featureCode,
                    actionCode,
                    menu.getPath(),
                    menu.getIcon(),
                    menu.getSortOrder(),
                    parentCode,
                    menu.getDescription(),
                    grantedCapabilities);

            items.add(item);

            // 자식 메뉴 재귀 처리 (부모가 표시되는 경우에만)
            for (MenuDefinition child : menu.getChildren()) {
                filterAndAddMenu(child, menu.getCode(), group, items);
            }
        }
    }

    /**
     * 사용자가 보유한 capability 중 메뉴에서 요구하는 것과 일치하는 capability 목록을 반환한다.
     */
    private List<MenuCapabilityRef> findGrantedCapabilities(MenuDefinition menu, PermissionGroup group) {
        List<MenuCapabilityRef> granted = new ArrayList<>();

        for (MenuDefinition.CapabilityRef required : menu.getRequiredCapabilities()) {
            try {
                FeatureCode feature = FeatureCode.valueOf(required.getFeature());
                ActionCode action = ActionCode.valueOf(required.getAction());

                if (group.assignmentFor(feature, action).isPresent()) {
                    granted.add(new MenuCapabilityRef(required.getFeature(), required.getAction()));
                }
            } catch (IllegalArgumentException e) {
                // 잘못된 FeatureCode/ActionCode는 무시
            }
        }

        return granted;
    }
}
