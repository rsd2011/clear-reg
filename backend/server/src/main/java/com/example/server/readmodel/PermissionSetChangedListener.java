package com.example.server.readmodel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.example.auth.permission.event.PermissionSetChangedEvent;
import com.example.dw.application.readmodel.OrganizationReadModelPort;

@Component
public class PermissionSetChangedListener {

    private static final Logger log = LoggerFactory.getLogger(PermissionSetChangedListener.class);

    private final OrganizationReadModelPort organizationReadModelPort;

    public PermissionSetChangedListener(@Nullable OrganizationReadModelPort organizationReadModelPort) {
        this.organizationReadModelPort = organizationReadModelPort;
    }

    @EventListener
    public void onPermissionChanged(PermissionSetChangedEvent event) {
        if (organizationReadModelPort == null || !organizationReadModelPort.isEnabled()) {
            log.debug("Organization read model disabled; skipping rebuild on permission change");
            return;
        }
        organizationReadModelPort.rebuild();
        log.info("Organization read model rebuilt due to permission change (principal={})", event.principalId());
    }
}
