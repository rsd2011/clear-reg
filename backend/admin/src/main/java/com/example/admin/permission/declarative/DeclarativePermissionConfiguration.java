package com.example.admin.permission.declarative;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DeclarativePermissionProperties.class)
class DeclarativePermissionConfiguration {

  @Bean(name = "permissionPolicyYamlMapper")
  ObjectMapper permissionPolicyYamlMapper() {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.registerModule(new JavaTimeModule());
    return mapper;
  }
}
