package com.example.common.masking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OutputMaskingAdapterTest {

    @Test
    @DisplayName("문자열에 PARTIAL 마스킹을 적용한다")
    void masksStringPartial() {
        MaskingTarget target = MaskingTarget.builder()
                .defaultMask(true)
                .maskRule("PARTIAL")
                .build();

        String masked = OutputMaskingAdapter.mask("account", "12345678", target, "PARTIAL", null);

        assertThat(masked).isEqualTo("12****78");
    }

    @Test
    @DisplayName("Maskable 값객체에도 masker를 적용한다")
    void masksMaskable() {
        MaskingTarget target = MaskingTarget.builder()
                .maskRule("HASH")
                .build();
        Maskable value = new DummyMaskable("9876543210");

        String masked = OutputMaskingAdapter.mask("rrn", value, target, "HASH", null);

        assertThat(masked).hasSize(64); // SHA-256 hex length
    }

    @Test
    @DisplayName("forceUnmaskFields가 설정되면 원문을 반환한다")
    void forceUnmaskFieldReturnsRaw() {
        MaskingTarget target = MaskingTarget.builder()
                .forceUnmaskFields(Set.of("ownerName"))
                .maskRule("FULL")
                .build();

        String masked = OutputMaskingAdapter.mask("ownerName", "홍길동", target, "FULL", null);

        assertThat(masked).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("forceUnmaskKinds가 dataKind에 매칭되면 원문을 반환한다")
    void forceUnmaskKindReturnsRaw() {
        MaskingTarget target = MaskingTarget.builder()
                .dataKind("RRN")
                .forceUnmaskKinds(Set.of("RRN"))
                .maskRule("FULL")
                .build();

        String masked = OutputMaskingAdapter.mask("rrn", "900101-1234567", target, "FULL", null);

        assertThat(masked).isEqualTo("900101-1234567");
    }

    @Test
    @DisplayName("CSV 셀 값도 동일한 마스킹 규칙이 적용된다")
    void csvCellMasking() {
        MaskingTarget target = MaskingTarget.builder()
                .maskRule("PARTIAL")
                .dataKind("ACCOUNT")
                .build();

        String masked = OutputMaskingAdapter.mask("accountNumber", "1234-5678-9012-3456", target, "PARTIAL", null);

        assertThat(masked).startsWith("12").contains("*").endsWith("56");
    }

    @Test
    @DisplayName("JSON 필드 값에도 Hash 마스킹을 적용한다")
    void jsonFieldMasking() {
        MaskingTarget target = MaskingTarget.builder()
                .maskRule("HASH")
                .dataKind("EMAIL")
                .build();

        String masked = OutputMaskingAdapter.mask("email", "user@example.com", target, "HASH", null);

        assertThat(masked).hasSize(64);
        assertThat(masked).isNotEqualTo("user@example.com");
    }

    private record DummyMaskable(String raw) implements Maskable {
        @Override public String masked() { return "[MASKED]"; }
    }
}
