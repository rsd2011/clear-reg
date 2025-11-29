package com.example.admin.maskingpolicy.masking;

import com.example.admin.maskingpolicy.service.MaskingEvaluator;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class SensitiveDataMaskingModule extends SimpleModule {

  public SensitiveDataMaskingModule(MaskingEvaluator evaluator) {
    super("SensitiveDataMaskingModule");
    setSerializerModifier(new SensitiveSerializerModifier(evaluator));
  }
}
