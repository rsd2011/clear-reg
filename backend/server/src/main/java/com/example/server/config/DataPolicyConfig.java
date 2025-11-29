package com.example.server.config;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.admin.maskingpolicy.masking.SensitiveDataMaskingModule;

@Configuration
public class DataPolicyConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer sensitiveCustomizer(SensitiveDataMaskingModule module) {
        return builder -> builder.modulesToInstall(module);
    }
}
