package com.example.admin.datapolicy.masking;

import com.example.admin.datapolicy.service.DataPolicyEvaluator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;

@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "SE_BAD_FIELD"},
    justification = "Custom writer keeps evaluator reference intentionally; serializer is not used in Java serialization")
class SensitivePropertyWriter extends BeanPropertyWriter {

  private final DataPolicyEvaluator evaluator;
  private final String tag;

  SensitivePropertyWriter(BeanPropertyWriter base, DataPolicyEvaluator evaluator, String tag) {
    super(base);
    this.evaluator = evaluator;
    this.tag = tag;
  }

  @Override
  public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov)
      throws Exception {
    Object rawValue = get(bean);
    Object masked = evaluator.mask(tag, rawValue);
    if (masked == rawValue) {
      super.serializeAsField(bean, gen, prov);
      return;
    }
    writeMaskedField(masked, bean, gen, prov);
  }

  private void writeMaskedField(
      Object value, Object bean, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    if (value == null) {
      if (this._nullSerializer != null) {
        gen.writeFieldName(this._name);
        this._nullSerializer.serialize(null, gen, provider);
      } else {
        try {
          super.serializeAsOmittedField(bean, gen, provider);
        } catch (Exception exception) {
          throw new IOException("Failed to serialize masked field", exception);
        }
      }
      return;
    }
    gen.writeFieldName(this._name);
    provider.defaultSerializeValue(value, gen);
  }
}
