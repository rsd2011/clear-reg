package com.example.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FeatureCode 테스트")
class FeatureCodeTest {

  @Test
  @DisplayName("Given 모든 FeatureCode 상수 When values 호출 Then 모든 상수가 반환된다")
  void givenAllFeatureCodes_whenValues_thenReturnsAllConstants() {
    FeatureCode[] values = FeatureCode.values();

    assertThat(values).hasSize(17);
    assertThat(values).contains(
        FeatureCode.RULE_MANAGE,
        FeatureCode.CUSTOMER,
        FeatureCode.AUDIT_LOG,
        FeatureCode.ORGANIZATION,
        FeatureCode.EMPLOYEE,
        FeatureCode.MENU,
        FeatureCode.POLICY,
        FeatureCode.HR_IMPORT,
        FeatureCode.NOTICE,
        FeatureCode.FILE,
        FeatureCode.ALERT,
        FeatureCode.COMMON_CODE,
        FeatureCode.DRAFT,
        FeatureCode.DRAFT_TEMPLATE,
        FeatureCode.APPROVAL,
        FeatureCode.APPROVAL_MANAGE,
        FeatureCode.USER
    );
  }

  @Test
  @DisplayName("Given FeatureCode 문자열 When valueOf 호출 Then 해당 상수 반환")
  void givenFeatureCodeString_whenValueOf_thenReturnsConstant() {
    assertThat(FeatureCode.valueOf("ORGANIZATION")).isEqualTo(FeatureCode.ORGANIZATION);
    assertThat(FeatureCode.valueOf("DRAFT")).isEqualTo(FeatureCode.DRAFT);
    assertThat(FeatureCode.valueOf("APPROVAL")).isEqualTo(FeatureCode.APPROVAL);
  }

  @Test
  @DisplayName("Given FeatureCode 상수 When name 호출 Then 상수명 문자열 반환")
  void givenFeatureCode_whenName_thenReturnsConstantName() {
    assertThat(FeatureCode.ORGANIZATION.name()).isEqualTo("ORGANIZATION");
    assertThat(FeatureCode.DRAFT.name()).isEqualTo("DRAFT");
  }
}
