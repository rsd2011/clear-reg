package com.example.common.masking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.policy.DataPolicyMatch;

class OutputMaskingAdapterE2eTest {

    @Test
    @DisplayName("forceUnmask가 없을 때 마스킹 규칙이 모든 포맷에 동일하게 적용된다")
    void maskAppliesRule() {
        MaskingTarget target = MaskingTarget.builder()
                .subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                .dataKind("accountNumber")
                .build();

        String masked = OutputMaskingAdapter.mask("accountNumber", "1234567890123456",
                target, "PARTIAL", "{\"keep\":4}");

        assertThat(masked).isEqualTo("************3456");
    }

    @Test
    @DisplayName("forceUnmaskFields가 지정되면 해당 필드는 원문이 유지된다")
    void forceUnmaskFieldWins() {
        MaskingTarget target = MaskingTarget.builder()
                .forceUnmaskFields(Set.of("accountNumber"))
                .dataKind("accountNumber")
                .build();

        String masked = OutputMaskingAdapter.mask("accountNumber", "123-45-67890",
                target, "FULL", null);

        assertThat(masked).isEqualTo("123-45-67890");
    }

    @Test
    @DisplayName("Maskable 값객체도 동일한 규칙으로 마스킹된다")
    void maskableRespectsRule() {
        MaskingTarget target = MaskingTarget.builder()
                .dataKind("rrn")
                .build();

        Maskable rrn = () -> "900101-1234567";

        String masked = OutputMaskingAdapter.mask("rrn", rrn, target, "PARTIAL", "{\"keep\":2}");

        assertThat(masked).startsWith("**").endsWith("67");
    }
}
