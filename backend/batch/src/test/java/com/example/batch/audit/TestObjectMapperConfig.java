package com.example.batch.audit;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

@TestConfiguration
public class TestObjectMapperConfig {

    @Bean
    @Primary
    public ObjectMapper primaryObjectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder.build();
    }
}
