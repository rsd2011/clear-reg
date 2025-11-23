package com.example.audit.infra.masking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MaskingProperties 기본/커스텀 규칙 적용")
class MaskingPropertiesTest {

    @Test
    void applyAll_withDefaultsAndOverride() {
        MaskingProperties props = new MaskingProperties();
        // 기본 규칙 적용
        String masked = props.applyAll("주민번호 990101-1234567");
        assertThat(masked).contains("[REDACTED-RRN]");

        // 커스텀 규칙으로 교체
        MaskingRule custom = new MaskingRule("ABC", "XXX");
        props.setRules(List.of(custom));
        assertThat(props.getRules()).containsExactly(custom);
        assertThat(props.applyAll("ABC DEF")).contains("XXX");
    }
}

