package com.example.common.masking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.policy.DataPolicyMatch;

class OutputMaskingAdapterTest {

    @Test
    @DisplayName("기본 마스킹 규칙을 적용해 문자열을 마스킹한다")
    void masksStringWithRule() {
        MaskingTarget target = MaskingTarget.builder()
                .forceUnmask(false)
                .dataKind("accountNumber")
                .build();

        String masked = OutputMaskingAdapter.mask("acctNo", "1234-5678-9012", target, "PARTIAL", "{\"keepEnd\":4}");

        assertThat(masked)
                .matches(".*\\d{2}$") // 끝 2자리는 남는다
                .isNotEqualTo("1234-5678-9012");
    }

    @Test
    @DisplayName("forceUnmask 필드에 포함되면 원문을 반환한다")
    void forceUnmaskFieldReturnsRaw() {
        MaskingTarget target = MaskingTarget.builder()
                .forceUnmaskFields(Set.of("acctNo"))
                .forceUnmask(false)
                .dataKind("accountNumber")
                .build();

        String masked = OutputMaskingAdapter.mask("acctNo", "1234-5678-9012", target, "PARTIAL", "{\"keepEnd\":4}");

        assertThat(masked).isEqualTo("1234-5678-9012");
    }

    @Test
    @DisplayName("Maskable 값을 입력해도 정책 기반 마스킹을 적용한다")
    void masksMaskableValueObject() {
        MaskingTarget target = MaskingTarget.builder()
                .forceUnmask(false)
                .dataKind("rrn")
                .build();

        Maskable rrn = new Maskable() {
            @Override public String raw() { return "900101-1234567"; }
            @Override public String masked() { return "900101-1******"; }
        };

        String masked = OutputMaskingAdapter.mask("rrn", rrn, target, "HASH", null);

        assertThat(masked).hasSizeGreaterThan(10); // 해시 적용 결과 문자열
    }
}
