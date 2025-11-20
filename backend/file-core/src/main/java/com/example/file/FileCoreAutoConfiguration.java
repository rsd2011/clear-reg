package com.example.file;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.file.port.FileScanner;
import com.example.file.port.NoOpFileScanner;
import com.example.file.audit.FileAuditPublisher;
import com.example.file.audit.NoOpFileAuditPublisher;

@Configuration
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
