package com.example.admin.datapolicy.config;

import com.example.admin.datapolicy.service.DataPolicyEvaluator;
import com.example.admin.datapolicy.masking.SensitiveDataMaskingModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataPolicyConfiguration {

  @Bean
  public SensitiveDataMaskingModule sensitiveDataMaskingModule(DataPolicyEvaluator evaluator) {
    return new SensitiveDataMaskingModule(evaluator);
  }
}
