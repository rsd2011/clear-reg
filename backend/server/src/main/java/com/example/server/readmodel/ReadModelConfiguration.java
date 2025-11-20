package com.example.server.readmodel;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        OrganizationReadModelProperties.class,
        MenuReadModelProperties.class,
        PermissionMenuReadModelProperties.class
})
public class ReadModelConfiguration {
}
