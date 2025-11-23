package com.example.audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "audit.retention")
public record AuditRetentionProperties(@DefaultValue("730") int days) {
}
