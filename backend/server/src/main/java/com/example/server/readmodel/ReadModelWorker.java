package com.example.server.readmodel;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.dw.application.readmodel.MenuReadModelPort;
import com.example.dw.application.readmodel.OrganizationReadModelPort;
import com.example.dw.application.readmodel.PermissionMenuReadModelPort;

/**
 * 단순 read-model worker: 필요한 시점에 전체 또는 특정 principal 대상 read model을 재생성한다.
 * 후속 단계에서 큐/스케줄 기반 트리거로 확장할 수 있다.
 */
public class ReadModelWorker {

    private static final Logger log = LoggerFactory.getLogger(ReadModelWorker.class);

    private final OrganizationReadModelPort organizationReadModelPort;
    private final MenuReadModelPort menuReadModelPort;
    private final PermissionMenuReadModelPort permissionMenuReadModelPort;

    public ReadModelWorker(OrganizationReadModelPort organizationReadModelPort,
                           MenuReadModelPort menuReadModelPort,
                           PermissionMenuReadModelPort permissionMenuReadModelPort) {
        this.organizationReadModelPort = organizationReadModelPort;
        this.menuReadModelPort = menuReadModelPort;
        this.permissionMenuReadModelPort = permissionMenuReadModelPort;
    }

    public void rebuildOrganization() {
        if (organizationReadModelPort != null && organizationReadModelPort.isEnabled()) {
            organizationReadModelPort.rebuild();
            log.info("Rebuilt organization read model");
        }
    }

    public void rebuildMenu() {
        if (menuReadModelPort != null && menuReadModelPort.isEnabled()) {
            menuReadModelPort.rebuild();
            log.info("Rebuilt menu read model");
        }
    }

    public void rebuildPermissionMenu(String principalId) {
        if (permissionMenuReadModelPort != null && permissionMenuReadModelPort.isEnabled()) {
            permissionMenuReadModelPort.rebuild(principalId);
            log.info("Rebuilt permission menu read model for principal={}", principalId);
        }
    }

    public Optional<com.example.dw.application.readmodel.OrganizationTreeReadModel> loadOrganization() {
        if (organizationReadModelPort != null && organizationReadModelPort.isEnabled()) {
            return organizationReadModelPort.load();
        }
        return Optional.empty();
    }

    public Optional<com.example.dw.application.readmodel.MenuReadModel> loadMenu() {
        if (menuReadModelPort != null && menuReadModelPort.isEnabled()) {
            return menuReadModelPort.load();
        }
        return Optional.empty();
    }

    public Optional<com.example.dw.application.readmodel.PermissionMenuReadModel> loadPermissionMenu(String principalId) {
        if (permissionMenuReadModelPort != null && permissionMenuReadModelPort.isEnabled()) {
            return permissionMenuReadModelPort.load(principalId);
        }
        return Optional.empty();
    }
}
