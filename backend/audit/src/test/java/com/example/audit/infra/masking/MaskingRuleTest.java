package com.example.audit.infra.masking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MaskingRule 적용/지연컴파일")
class MaskingRuleTest {

    @Test
    void apply_withNullAndLazyCompile() {
        MaskingRule rule = new MaskingRule("123", "XYZ");
        assertThat(rule.apply(null)).isNull();
        assertThat(rule.apply("123-123")).isEqualTo("XYZ-XYZ");

        // setter로 패턴 변경 시 재컴파일
        rule.setPattern("abc");
        rule.setReplacement("DEF");
        assertThat(rule.apply("xxabcxx")).contains("DEF");

        // getter 커버리지
        assertThat(rule.getPattern()).isEqualTo("abc");
        assertThat(rule.getReplacement()).isEqualTo("DEF");
    }
}
