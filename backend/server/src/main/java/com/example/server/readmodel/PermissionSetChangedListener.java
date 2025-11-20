package com.example.server.readmodel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.example.auth.permission.event.PermissionSetChangedEvent;
import com.example.dw.application.readmodel.OrganizationReadModelPort;
import com.example.dw.application.readmodel.MenuReadModelPort;

@Component
public class PermissionSetChangedListener {

    private static final Logger log = LoggerFactory.getLogger(PermissionSetChangedListener.class);

    private final OrganizationReadModelPort organizationReadModelPort;
    private final MenuReadModelPort menuReadModelPort;

    public PermissionSetChangedListener(@Nullable OrganizationReadModelPort organizationReadModelPort,
                                        @Nullable MenuReadModelPort menuReadModelPort) {
        this.organizationReadModelPort = organizationReadModelPort;
        this.menuReadModelPort = menuReadModelPort;
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
    }
}
