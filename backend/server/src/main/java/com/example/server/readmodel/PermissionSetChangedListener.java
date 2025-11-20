package com.example.server.readmodel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.example.auth.permission.event.PermissionSetChangedEvent;
import com.example.dw.application.readmodel.OrganizationReadModelPort;
import com.example.dw.application.readmodel.MenuReadModelPort;
import com.example.dw.application.readmodel.PermissionMenuReadModelPort;

@Component
public class PermissionSetChangedListener {

    private static final Logger log = LoggerFactory.getLogger(PermissionSetChangedListener.class);

    private final OrganizationReadModelPort organizationReadModelPort;
    private final MenuReadModelPort menuReadModelPort;
    private final PermissionMenuReadModelPort permissionMenuReadModelPort;

    public PermissionSetChangedListener(@Nullable OrganizationReadModelPort organizationReadModelPort,
                                        @Nullable MenuReadModelPort menuReadModelPort,
                                        @Nullable PermissionMenuReadModelPort permissionMenuReadModelPort) {
        this.organizationReadModelPort = organizationReadModelPort;
        this.menuReadModelPort = menuReadModelPort;
        this.permissionMenuReadModelPort = permissionMenuReadModelPort;
    }

    @EventListener
    public void onPermissionChanged(PermissionSetChangedEvent event) {
        if (organizationReadModelPort == null || !organizationReadModelPort.isEnabled()) {
            log.debug("Organization read model disabled; skipping rebuild on permission change");
            return;
        }
        organizationReadModelPort.rebuild();
        log.info("Organization read model rebuilt due to permission change (principal={})", event.principalId());

        if (menuReadModelPort != null && menuReadModelPort.isEnabled()) {
            menuReadModelPort.rebuild();
            log.info("Menu read model rebuilt due to permission change (principal={})", event.principalId());
        }

        if (permissionMenuReadModelPort != null && permissionMenuReadModelPort.isEnabled() && event.principalId() != null) {
            permissionMenuReadModelPort.rebuild(event.principalId());
            log.info("Permission menu read model rebuilt due to permission change (principal={})", event.principalId());
        }
    }
}
