package com.example.auth.policy;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class SensitiveDataMaskingModule extends SimpleModule {

    public SensitiveDataMaskingModule(DataPolicyEvaluator evaluator) {
        super("SensitiveDataMaskingModule");
        setSerializerModifier(new SensitiveSerializerModifier(evaluator));
    }
}
