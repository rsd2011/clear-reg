package com.example.admin.masking;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.FieldMaskRule;
import com.example.admin.permission.context.AuthCurrentUserProvider;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.annotation.Sensitive;
import com.example.common.security.RowScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SensitiveSerialization 테스트")
class SensitiveSerializationTest {

  private final DataPolicyEvaluator evaluator = new DataPolicyEvaluator(new AuthCurrentUserProvider());

  @AfterEach
  void cleanup() {
    AuthContextHolder.clear();
  }

  @Test
  @DisplayName("Given 민감 필드 When 직렬화 Then 권한에 따라 마스킹 여부가 결정된다")
  void givenSensitiveField_whenSerializing_thenMaskUnlessAllowed() throws Exception {
    FieldMaskRule rule = new FieldMaskRule("SECRET", "MASKED", ActionCode.UNMASK, true);
    AuthContextHolder.set(
        new AuthContext(
            "auditor",
            "ORG",
            "AUDIT",
            FeatureCode.ORGANIZATION,
            ActionCode.UNMASK,
            RowScope.ALL,
            Map.of("SECRET", rule)));

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new SensitiveDataMaskingModule(evaluator));

    SensitiveBean payload = new SensitiveBean("token", "visible");
    String json = mapper.writeValueAsString(payload);
    assertThat(json).contains("\"secret\":\"token\"");
    assertThat(json).contains("\"plain\":\"visible\"");
  }

  @Test
  @DisplayName("Given READ 권한만 있는 경우 When 직렬화 Then 마스킹된 값으로 출력된다")
  void givenReadOnlyContext_whenSerializing_thenMaskSecret() throws Exception {
    FieldMaskRule rule = new FieldMaskRule("SECRET", "MASKED", ActionCode.UNMASK, true);
    AuthContextHolder.set(
        new AuthContext(
            "auditor",
            "ORG",
            "AUDIT",
            FeatureCode.ORGANIZATION,
            ActionCode.READ,
            RowScope.ALL,
            Map.of("SECRET", rule)));

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new SensitiveDataMaskingModule(evaluator));

    SensitiveBean payload = new SensitiveBean("token", "visible");
    String json = mapper.writeValueAsString(payload);
    assertThat(json).contains("\"secret\":\"MASKED\"");
  }

  private static final class SensitiveBean {

    @Sensitive("SECRET")
    private final String secret;

    private final String plain;

    private SensitiveBean(String secret, String plain) {
      this.secret = secret;
      this.plain = plain;
    }

    public String getSecret() {
      return secret;
    }

    public String getPlain() {
      return plain;
    }
  }

  @Test
  @DisplayName("Given 데이터 정책 설정 When MaskingModule 생성 Then 모듈을 재사용할 수 있다")
  void givenConfiguration_whenCreatingModule_thenReusable() {
    DataPolicyConfiguration configuration = new DataPolicyConfiguration();
    SensitiveDataMaskingModule module =
        configuration.sensitiveDataMaskingModule(new DataPolicyEvaluator(new AuthCurrentUserProvider()));
    assertThat(module).isNotNull();
  }
}
