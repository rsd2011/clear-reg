package com.example.common.masking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MaskingInfrastructureTest {

    @Test
    @DisplayName("Given 규칙 값 When MaskRuleProcessor 적용 Then FULL/PARTIAL/HASH/TOKENIZE 별로 기대값이 반환된다")
    void maskRuleProcessorVariants() {
        assertThat(MaskRuleProcessor.apply("FULL", "secret", null)).isEqualTo("[MASKED]");
        assertThat(MaskRuleProcessor.apply("PARTIAL", "abcdef", null)).isEqualTo("ab**ef");
        assertThat(MaskRuleProcessor.apply("HASH", "abc", null)).hasSize(64);
        assertThat(MaskRuleProcessor.apply("TOKENIZE", "abc", null)).contains("-");
        assertThat(MaskRule.of("hash")).isEqualTo(MaskRule.HASH);
        assertThat(MaskRule.of("unknown")).isEqualTo(MaskRule.NONE);
    }

    @Test
    @DisplayName("Given MaskingTarget When ContextHolder set/get/clear Then 값이 설정·조회·해제된다")
    void maskingContextHolderLifecycle() {
        MaskingTarget target = MaskingTarget.builder()
                .subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                .dataKind(DataKind.SSN)
                .defaultMask(true)
                .build();
        MaskingContextHolder.set(target);
        assertThat(MaskingContextHolder.get()).isEqualTo(target);
        MaskingContextHolder.clear();
        assertThat(MaskingContextHolder.get()).isNull();
    }
}
