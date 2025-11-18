package com.example.auth.policy;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

public class SensitiveSerializerModifier extends BeanSerializerModifier {

    private final DataPolicyEvaluator evaluator;

    public SensitiveSerializerModifier(DataPolicyEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                     BeanDescription beanDesc,
                                                     List<BeanPropertyWriter> beanProperties) {
        List<BeanPropertyWriter> writers = new ArrayList<>(beanProperties.size());
        for (BeanPropertyWriter writer : beanProperties) {
            Sensitive sensitive = writer.getAnnotation(Sensitive.class);
            if (sensitive != null) {
                writers.add(new SensitivePropertyWriter(writer, evaluator, sensitive.value()));
            } else {
                writers.add(writer);
            }
        }
        return writers;
    }
}
