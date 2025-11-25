package com.example.auth.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.example.common.annotation.Sensitive;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SensitiveSerializerModifierTest {

  static class MaskedBean {
    @Sensitive("TAG")
    public String secret = "plain";

    public String normal = "open";
  }

  private ObjectMapper mapperWith(DataPolicyEvaluator evaluator) {
    ObjectMapper mapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    module.setSerializerModifier(new SensitiveSerializerModifier(evaluator));
    mapper.registerModule(module);
    return mapper;
  }

  @Test
  @DisplayName("마스킹 결과가 원본과 같으면 필드가 그대로 직렬화된다")
  void sameValueKeepsOriginal() throws Exception {
    DataPolicyEvaluator evaluator = mock(DataPolicyEvaluator.class);
    given(evaluator.mask(any(), any())).willReturn("plain");
    String json = mapperWith(evaluator).writeValueAsString(new MaskedBean());
    assertThat(json).contains("plain").contains("normal");
  }

  @Test
  @DisplayName("마스킹 결과가 null이면 필드가 null로 직렬화된다")
  void nullMaskOmitsField() throws Exception {
    DataPolicyEvaluator evaluator = mock(DataPolicyEvaluator.class);
    given(evaluator.mask(any(), any())).willReturn(null);
    String json = mapperWith(evaluator).writeValueAsString(new MaskedBean());
    assertThat(json).contains("\"secret\":null");
  }

  @Test
  @DisplayName("마스킹 결과가 다르면 마스킹된 값이 직렬화된다")
  void maskedValueIsSerialized() throws Exception {
    DataPolicyEvaluator evaluator = mock(DataPolicyEvaluator.class);
    given(evaluator.mask(any(), any())).willReturn("****");
    String json = mapperWith(evaluator).writeValueAsString(new MaskedBean());
    assertThat(json).contains("****");
  }

  @Test
  @DisplayName("마스킹 결과가 null이고 null serializer가 있으면 null serializer로 직렬화된다")
  void nullMaskUsesNullSerializer() throws Exception {
    DataPolicyEvaluator evaluator = mock(DataPolicyEvaluator.class);
    given(evaluator.mask(any(), any())).willReturn(null);
    ObjectMapper mapper = mapperWith(evaluator);
    mapper
        .getSerializerProvider()
        .setNullValueSerializer(
            new com.fasterxml.jackson.databind.ser.std.ToStringSerializer(Object.class));
    mapper
        .getSerializerProvider()
        .setNullValueSerializer(
            new com.fasterxml.jackson.databind.JsonSerializer<>() {
              @Override
              public void serialize(
                  Object value,
                  com.fasterxml.jackson.core.JsonGenerator gen,
                  com.fasterxml.jackson.databind.SerializerProvider serializers)
                  throws java.io.IOException {
                gen.writeString("N/A");
              }
            });

    String json = mapper.writeValueAsString(new MaskedBean());

    assertThat(json).contains("\"secret\":\"N/A\"");
  }

  @Test
  @DisplayName("마스킹 결과가 원본과 같으면 BeanPropertyWriter를 그대로 사용한다")
  void sameValueDelegatesToWriter() throws Exception {
    DataPolicyEvaluator evaluator = mock(DataPolicyEvaluator.class);
    given(evaluator.mask(any(), any())).willAnswer(inv -> inv.getArgument(1)); // raw == masked

    String json = mapperWith(evaluator).writeValueAsString(new MaskedBean());

    assertThat(json).contains("\"secret\":\"plain\"").contains("\"normal\":\"open\"");
  }

  @Test
  @DisplayName("마스킹 결과가 null이고 null serializer가 없으면 기본 null 직렬화로 필드가 null로 기록된다")
  void nullMaskDefaultsToNull() throws Exception {
    DataPolicyEvaluator evaluator = mock(DataPolicyEvaluator.class);
    given(evaluator.mask(any(), any())).willReturn(null);
    ObjectMapper mapper = mapperWith(evaluator); // null serializer 설정 안 함

    String json = mapper.writeValueAsString(new MaskedBean());

    assertThat(json).contains("\"secret\":null");
    assertThat(json).contains("\"normal\":\"open\"");
  }
}
