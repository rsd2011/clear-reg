package com.example.dw.application.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.masking.MaskingTarget;

class PdfMaskingAdapterTest {

    @Test
    @DisplayName("마스킹된 문자열을 consumer로 전달한다")
    void writesMaskedParagraph() {
        AtomicReference<String> captured = new AtomicReference<>();

        MaskingTarget target = MaskingTarget.builder()
                .dataKind("accountNumber")
                .build();

        PdfMaskingAdapter.writeMaskedParagraph(
                Map.of("accountNumber", "1234-5678-9012"),
                target,
                "PARTIAL",
                "{\"keepEnd\":4}",
                captured::set);

        assertThat(captured.get()).doesNotContain("1234-5678-9012");
    }
}
