package com.example.admin.maskingpolicy.config;

import com.example.admin.maskingpolicy.masking.SensitiveDataMaskingModule;
import com.example.admin.maskingpolicy.service.MaskingEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MaskingPolicyConfiguration {

  @Bean
  public SensitiveDataMaskingModule sensitiveDataMaskingModule(MaskingEvaluator evaluator) {
    return new SensitiveDataMaskingModule(evaluator);
  }
}
