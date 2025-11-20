package com.example.file;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.example.file.port.FileScanner;
import com.example.file.port.NoOpFileScanner;
import com.example.file.audit.FileAuditPublisher;
import com.example.file.audit.NoOpFileAuditPublisher;
import com.example.file.config.FileSecurityProperties;

@Configuration
@EnableConfigurationProperties(FileSecurityProperties.class)
@EnableScheduling
public class FileCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(FileScanner.class)
    public FileScanner fileScanner() {
        return new NoOpFileScanner();
    }

    @Bean
    @ConditionalOnMissingBean(FileAuditPublisher.class)
    public FileAuditPublisher fileAuditPublisher() {
        return new NoOpFileAuditPublisher();
    }
}
