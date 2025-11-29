package com.example.common.masking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OutputMaskingAdapterTest {

    @Test
    @DisplayName("마스킹 활성화 시 DataKind 기반 규칙을 적용해 문자열을 마스킹한다")
    void masksStringWithRule() {
        MaskingTarget target = MaskingTarget.builder()
                .forceUnmask(false)
                .dataKind(DataKind.ACCOUNT_NO)
                .build();

        String masked = OutputMaskingAdapter.mask("acctNo", "1234-5678-9012", target, true);

        // ACCOUNT_NO의 기본 규칙은 PARTIAL (앞 2자리, 뒤 2자리 유지)
        assertThat(masked).startsWith("12");
        assertThat(masked).endsWith("12");
        assertThat(masked).isNotEqualTo("1234-5678-9012");
    }

    @Test
    @DisplayName("forceUnmask 필드에 포함되면 원문을 반환한다")
    void forceUnmaskFieldReturnsRaw() {
        MaskingTarget target = MaskingTarget.builder()
                .forceUnmaskFields(Set.of("acctNo"))
                .forceUnmask(false)
                .dataKind(DataKind.ACCOUNT_NO)
                .build();

        String masked = OutputMaskingAdapter.mask("acctNo", "1234-5678-9012", target, true);

        assertThat(masked).isEqualTo("1234-5678-9012");
    }

    @Test
    @DisplayName("Maskable 값을 입력해도 DataKind 기반 마스킹을 적용한다")
    void masksMaskableValueObject() {
        MaskingTarget target = MaskingTarget.builder()
                .forceUnmask(false)
                .dataKind(DataKind.SSN)
                .build();

        Maskable<String> rrn = new Maskable<String>() {
            @Override public String raw() { return "900101-1234567"; }
            @Override public String masked() { return "900101-1******"; }
            @Override public DataKind dataKind() { return DataKind.SSN; }
        };

        String masked = OutputMaskingAdapter.mask("rrn", rrn, target, true);

        // DataKind.SSN의 기본 규칙은 FULL이므로 마스킹된 결과
        assertThat(masked).isEqualTo("[MASKED]");
    }

    @Test
    @DisplayName("maskingEnabled가 false면 원본을 반환한다 (화이트리스트)")
    void whitelist_returnsRaw() {
        MaskingTarget target = MaskingTarget.builder()
                .forceUnmask(false)
                .dataKind(DataKind.ACCOUNT_NO)
                .build();

        Maskable<String> account = new Maskable<String>() {
            @Override public String raw() { return "1234567890"; }
            @Override public String masked() { return "12***90"; }
            @Override public DataKind dataKind() { return DataKind.ACCOUNT_NO; }
        };

        String result = OutputMaskingAdapter.mask("acctNo", account, target, false);

        assertThat(result).isEqualTo("1234567890");
    }
}
