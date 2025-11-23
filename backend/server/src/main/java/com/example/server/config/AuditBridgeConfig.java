package com.example.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.audit.AuditPort;
import com.example.audit.drm.DrmAuditService;

/**
 * Bridge configuration to expose audit module beans (e.g. DRM audit) into the
 * server context without broad component scanning across modules.
 */
@Configuration
public class AuditBridgeConfig {

    @Bean
    public DrmAuditService drmAuditService(AuditPort auditPort) {
        return new DrmAuditService(auditPort);
    }
}
