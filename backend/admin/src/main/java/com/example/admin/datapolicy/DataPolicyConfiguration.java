package com.example.admin.datapolicy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataPolicyConfiguration {

  @Bean
  public SensitiveDataMaskingModule sensitiveDataMaskingModule(DataPolicyEvaluator evaluator) {
    return new SensitiveDataMaskingModule(evaluator);
  }
}
