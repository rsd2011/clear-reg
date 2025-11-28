package com.example.admin.datapolicy.masking;

import com.example.admin.datapolicy.service.DataPolicyEvaluator;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class SensitiveDataMaskingModule extends SimpleModule {

  public SensitiveDataMaskingModule(DataPolicyEvaluator evaluator) {
    super("SensitiveDataMaskingModule");
    setSerializerModifier(new SensitiveSerializerModifier(evaluator));
  }
}
