package com.example.dw.application.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.masking.MaskingTarget;

class ExportMaskingHelperTest {

    @Test
    @DisplayName("행 데이터를 필드별로 마스킹한다")
    void masksRow() {
        MaskingTarget target = MaskingTarget.builder()
                .forceUnmask(false)
                .dataKind("accountNumber")
                .build();

        Map<String, Object> masked = ExportMaskingHelper.maskRow(
                Map.of("accountNumber", "1234-5678-9012", "name", "Kim"),
                target,
                "PARTIAL",
                "{\"keepEnd\":4}");

        assertThat(masked.get("accountNumber")).isNotEqualTo("1234-5678-9012");
        assertThat(masked.get("name")).isNotNull();
    }
}
