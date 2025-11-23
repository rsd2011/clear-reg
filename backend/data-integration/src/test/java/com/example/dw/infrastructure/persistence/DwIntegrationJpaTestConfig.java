package com.example.dw.infrastructure.persistence;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = "com.example.dw.domain")
@EnableJpaRepositories(basePackages = "com.example.dw.infrastructure.persistence")
class DwIntegrationJpaTestConfig {
}
