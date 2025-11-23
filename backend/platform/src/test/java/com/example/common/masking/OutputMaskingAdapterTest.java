package com.example.common.masking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OutputMaskingAdapterTest {

    @Test
    @DisplayName("MaskRule PARTIAL이 문자열에 적용된다")
    void maskStringPartial() {
        MaskingTarget target = MaskingTarget.builder()
                .subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                .dataKind("RRN")
                .defaultMask(true)
                .build();

        String masked = OutputMaskingAdapter.mask("residentId", "990101-1234567", target, "PARTIAL", null);

        assertThat(masked).doesNotContain("1234567");
        assertThat(masked.length()).isEqualTo("990101-1234567".length());
        assertThat(masked).startsWith("99");
    }

    @Test
    @DisplayName("Maskable 값객체에도 동일 규칙을 적용한다")
    void maskMaskable() {
        MaskingTarget target = MaskingTarget.builder()
                .subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                .dataKind("ACCOUNT")
                .defaultMask(true)
                .build();

        Maskable maskable = new Maskable() {
            @Override public String raw() { return "110-123-456789"; }
            @Override public String masked() { return "110-***-*****"; }
        };

        String masked = OutputMaskingAdapter.mask("accountNo", maskable, target, "HASH", null);

        assertThat(masked).doesNotContain("110-123-456789");
        assertThat(masked).hasSize(64); // SHA-256 해시 길이
    }

    @Test
    @DisplayName("forceUnmaskFields 지정 시 원문을 반환한다")
    void forceUnmaskFields() {
        MaskingTarget target = MaskingTarget.builder()
                .subjectType(SubjectType.EMPLOYEE)
                .dataKind("EXPORT")
                .defaultMask(true)
                .forceUnmaskFields(java.util.Set.of("name"))
                .build();

        String masked = OutputMaskingAdapter.mask("name", "홍길동", target, "FULL", null);
        assertThat(masked).isEqualTo("홍길동");
    }
}
