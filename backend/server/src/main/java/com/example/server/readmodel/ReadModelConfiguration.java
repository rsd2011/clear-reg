package com.example.server.readmodel;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OrganizationReadModelProperties.class)
public class ReadModelConfiguration {
}
