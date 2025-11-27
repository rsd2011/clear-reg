package com.example.server.readmodel;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountService;
import com.example.auth.organization.OrganizationPolicyService;
import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.PermissionAssignment;
import com.example.admin.permission.PermissionGroup;
import com.example.admin.permission.PermissionGroupService;
import com.example.dw.application.readmodel.PermissionMenuItem;
import com.example.dw.application.readmodel.PermissionMenuReadModel;
import com.example.dw.application.readmodel.PermissionMenuReadModelSource;

@Component
public class PermissionMenuReadModelSourceImpl implements PermissionMenuReadModelSource {

    private final UserAccountService userAccountService;
    private final PermissionGroupService permissionGroupService;
    private final OrganizationPolicyService organizationPolicyService;
    private final Clock clock;

    public PermissionMenuReadModelSourceImpl(UserAccountService userAccountService,
                                             PermissionGroupService permissionGroupService,
                                             OrganizationPolicyService organizationPolicyService,
                                             Clock clock) {
        this.userAccountService = userAccountService;
        this.permissionGroupService = permissionGroupService;
        this.organizationPolicyService = organizationPolicyService;
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

        List<PermissionMenuItem> items = new ArrayList<>();
        for (PermissionAssignment assignment : group.getAssignments()) {
            FeatureCode feature = assignment.getFeature();
            ActionCode action = assignment.getAction();
            String code = feature.name() + ":" + action.name();
            String name = feature.name();
            String path = "/" + feature.name().toLowerCase().replace('_', '-');
            Set<String> maskingTags = group.maskRulesByTag().keySet();
            items.add(new PermissionMenuItem(code, name, feature.name(), action.name(), path, maskingTags));
        }

        return new PermissionMenuReadModel(
                UUID.randomUUID().toString(),
                OffsetDateTime.now(clock),
                items);
    }
}
