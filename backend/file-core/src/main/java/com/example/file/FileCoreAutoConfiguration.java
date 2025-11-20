package com.example.file;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.file.port.FileScanner;
import com.example.file.port.NoOpFileScanner;

@Configuration
public class FileCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(FileScanner.class)
    public FileScanner fileScanner() {
        return new NoOpFileScanner();
    }
}
