package com.example.admin.maskingpolicy.masking;

import com.example.admin.maskingpolicy.service.MaskingEvaluator;
import com.example.common.annotation.Sensitive;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;

@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "SE_BAD_FIELD"},
    justification = "Holds shared evaluator; not subject to Java serialization")
public class SensitiveSerializerModifier extends BeanSerializerModifier {

  private final MaskingEvaluator evaluator;

  public SensitiveSerializerModifier(MaskingEvaluator evaluator) {
    this.evaluator = evaluator;
  }

  @Override
  public List<BeanPropertyWriter> changeProperties(
      SerializationConfig config,
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
