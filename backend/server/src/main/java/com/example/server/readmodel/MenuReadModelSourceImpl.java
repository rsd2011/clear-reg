package com.example.server.readmodel;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.PermissionAssignment;
import com.example.admin.permission.PermissionGroup;
import com.example.admin.permission.PermissionGroupRepository;
import com.example.dw.application.readmodel.MenuItem;
import com.example.dw.application.readmodel.MenuReadModel;
import com.example.dw.application.readmodel.MenuReadModelSource;

/**
 * PermissionGroup 기반 메뉴 Read Model 생성기. Feature/Action 조합을 메뉴 항목으로 취급한다.
 */
@Component
public class MenuReadModelSourceImpl implements MenuReadModelSource {

    private final PermissionGroupRepository permissionGroupRepository;
    private final Clock clock;

    public MenuReadModelSourceImpl(PermissionGroupRepository permissionGroupRepository, Clock clock) {
        this.permissionGroupRepository = permissionGroupRepository;
        this.clock = clock;
    }

    @Override
    public MenuReadModel snapshot() {
        List<PermissionGroup> groups = permissionGroupRepository.findAll();
        Map<String, MenuItem> items = new LinkedHashMap<>();
        for (PermissionGroup group : groups) {
            for (PermissionAssignment assignment : group.getAssignments()) {
                FeatureCode feature = assignment.getFeature();
                ActionCode action = assignment.getAction();
                String code = feature.name() + ":" + action.name();
                // 동일 feature/action이면 첫 항목만 유지
                items.putIfAbsent(code, new MenuItem(
                        code,
                        feature.name(),
                        feature.name(),
                        action.name(),
                        "/" + feature.name().toLowerCase().replace('_', '-')
                ));
            }
        }
        return new MenuReadModel(UUID.randomUUID().toString(), OffsetDateTime.now(clock), new ArrayList<>(items.values()));
    }
}
