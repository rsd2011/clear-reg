package com.example.common.masking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OutputMaskingAdapterE2eTest {

    @Test
    @DisplayName("forceUnmask가 없을 때 DataKind 기반 마스킹 규칙이 적용된다")
    void maskAppliesDataKindBasedRule() {
        MaskingTarget target = MaskingTarget.builder()
                .subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                .dataKind("ACCOUNT_NO")
                .build();

        // ACCOUNT_NO의 기본 규칙은 PARTIAL이므로 마스킹 적용
        String masked = OutputMaskingAdapter.mask("accountNumber", "1234567890123456",
                target, true);

        // PARTIAL 규칙은 앞 2자리와 뒤 2자리 유지, 나머지는 * 처리
        assertThat(masked).startsWith("12");
        assertThat(masked).endsWith("56");
        assertThat(masked).contains("*");
    }

    @Test
    @DisplayName("forceUnmaskFields가 지정되면 해당 필드는 원문이 유지된다")
    void forceUnmaskFieldWins() {
        MaskingTarget target = MaskingTarget.builder()
                .forceUnmaskFields(Set.of("accountNumber"))
                .dataKind("ACCOUNT_NO")
                .build();

        String masked = OutputMaskingAdapter.mask("accountNumber", "123-45-67890",
                target, true);

        assertThat(masked).isEqualTo("123-45-67890");
    }

    @Test
    @DisplayName("Maskable 값객체도 DataKind 기반 마스킹이 적용된다")
    void maskableRespectsDataKindRule() {
        MaskingTarget target = MaskingTarget.builder()
                .dataKind("SSN")
                .build();

        Maskable<String> rrn = new Maskable<String>() {
            @Override
            public String raw() {
                return "900101-1234567";
            }

            @Override
            public String masked() {
                return "*******-*****67";
            }

            @Override
            public DataKind dataKind() {
                return DataKind.SSN;
            }
        };

        // SSN의 기본 규칙은 FULL이므로 완전히 마스킹됨
        String masked = OutputMaskingAdapter.mask("rrn", rrn, target, true);

        assertThat(masked).doesNotContain("900101-1234567");
        assertThat(masked).isEqualTo("[MASKED]");
    }

    @Test
    @DisplayName("maskingEnabled가 false면 원본이 반환된다 (화이트리스트)")
    void whitelistReturnsRaw() {
        MaskingTarget target = MaskingTarget.builder()
                .dataKind("SSN")
                .build();

        Maskable<String> rrn = new Maskable<String>() {
            @Override
            public String raw() {
                return "900101-1234567";
            }

            @Override
            public String masked() {
                return "*******-*****67";
            }
        };

        // maskingEnabled=false면 원본 반환 (화이트리스트)
        String masked = OutputMaskingAdapter.mask("rrn", rrn, target, false);

        assertThat(masked).isEqualTo("900101-1234567");
    }
}
