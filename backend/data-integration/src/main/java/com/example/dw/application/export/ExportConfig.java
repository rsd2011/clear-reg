package com.example.dw.application.export;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExportConfig {

    @Bean
    public ExportFailureNotifier exportFailureNotifier() {
        return new LoggingExportFailureNotifier();
    }
}
