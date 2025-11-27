package com.example.admin.masking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.context.AuthCurrentUserProvider;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.annotation.Sensitive;
import com.example.common.policy.DataPolicyMatch;
import com.example.common.policy.DataPolicyProvider;
import com.example.common.security.RowScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SensitiveSerialization 테스트")
class SensitiveSerializationTest {

  private DataPolicyProvider dataPolicyProvider;
  private DataPolicyEvaluator evaluator;

  @BeforeEach
  void setUp() {
    dataPolicyProvider = mock(DataPolicyProvider.class);
    evaluator = new DataPolicyEvaluator(new AuthCurrentUserProvider(), dataPolicyProvider);
  }

  @AfterEach
  void cleanup() {
    AuthContextHolder.clear();
  }

  @Test
  @DisplayName("Given UNMASK 권한이 있는 경우 When 직렬화 Then 원본 값이 출력된다")
  void givenUnmaskPermission_whenSerializing_thenShowOriginal() throws Exception {
    // AuthContext 설정
    AuthContextHolder.set(AuthContext.of(
        "auditor", "ORG", "AUDIT",
        FeatureCode.ORGANIZATION, ActionCode.UNMASK, RowScope.ALL));

    // DataPolicy가 NONE 규칙 반환 (마스킹 없음)
    DataPolicyMatch match = DataPolicyMatch.builder()
        .policyId(UUID.randomUUID())
        .sensitiveTag("SECRET")
        .maskRule("NONE")
        .requiredActionCode("UNMASK")
        .auditEnabled(false)
        .build();
    given(dataPolicyProvider.evaluate(any())).willReturn(Optional.of(match));

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
    // AuthContext 설정 (READ 권한만)
    AuthContextHolder.set(AuthContext.of(
        "auditor", "ORG", "AUDIT",
        FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ALL));

    // DataPolicy가 FULL 마스킹 규칙 반환 (UNMASK 필요)
    DataPolicyMatch match = DataPolicyMatch.builder()
        .policyId(UUID.randomUUID())
        .sensitiveTag("SECRET")
        .maskRule("FULL")
        .requiredActionCode("UNMASK")
        .auditEnabled(false)
        .build();
    given(dataPolicyProvider.evaluate(any())).willReturn(Optional.of(match));

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new SensitiveDataMaskingModule(evaluator));

    SensitiveBean payload = new SensitiveBean("token", "visible");
    String json = mapper.writeValueAsString(payload);
    assertThat(json).contains("\"secret\":\"***\"");
  }

  @Test
  @DisplayName("Given 매칭되는 정책이 없는 경우 When 직렬화 Then 기본 마스킹 적용")
  void givenNoMatchingPolicy_whenSerializing_thenDefaultMask() throws Exception {
    AuthContextHolder.set(AuthContext.of(
        "user", "ORG", "USER_GROUP",
        FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ORG));

    given(dataPolicyProvider.evaluate(any())).willReturn(Optional.empty());

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new SensitiveDataMaskingModule(evaluator));

    SensitiveBean payload = new SensitiveBean("secret-value", "visible");
    String json = mapper.writeValueAsString(payload);
    assertThat(json).contains("\"secret\":\"***\"");
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
        configuration.sensitiveDataMaskingModule(evaluator);
    assertThat(module).isNotNull();
  }
}
