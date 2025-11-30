package com.example.admin.maskingpolicy.masking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.example.admin.maskingpolicy.config.MaskingPolicyConfiguration;
import com.example.admin.maskingpolicy.service.MaskingEvaluator;
import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.admin.permission.context.AuthCurrentUserProvider;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.annotation.Sensitive;
import com.example.common.policy.MaskingMatch;
import com.example.common.policy.MaskingPolicyProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SensitiveSerialization 테스트")
class SensitiveSerializationTest {

  private MaskingPolicyProvider maskingPolicyProvider;
  private MaskingEvaluator evaluator;

  @BeforeEach
  void setUp() {
    maskingPolicyProvider = mock(MaskingPolicyProvider.class);
    evaluator = new MaskingEvaluator(new AuthCurrentUserProvider(), maskingPolicyProvider);
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
        FeatureCode.ORGANIZATION, ActionCode.UNMASK, List.of()));

    // MaskingPolicy가 마스킹 비활성화 반환
    MaskingMatch match = MaskingMatch.builder()
        .policyId(UUID.randomUUID())
        .maskingEnabled(false)
        .auditEnabled(false)
        .build();
    given(maskingPolicyProvider.evaluate(any())).willReturn(Optional.of(match));

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
        FeatureCode.ORGANIZATION, ActionCode.READ, List.of()));

    // MaskingPolicy가 마스킹 활성화 반환
    MaskingMatch match = MaskingMatch.builder()
        .policyId(UUID.randomUUID())
        .maskingEnabled(true)
        .auditEnabled(false)
        .build();
    given(maskingPolicyProvider.evaluate(any())).willReturn(Optional.of(match));

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
        FeatureCode.ORGANIZATION, ActionCode.READ, List.of()));

    given(maskingPolicyProvider.evaluate(any())).willReturn(Optional.empty());

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
    MaskingPolicyConfiguration configuration = new MaskingPolicyConfiguration();
    SensitiveDataMaskingModule module =
        configuration.sensitiveDataMaskingModule(evaluator);
    assertThat(module).isNotNull();
  }
}
