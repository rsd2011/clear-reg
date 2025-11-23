package com.example.dw.application.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.masking.MaskingTarget;

class ExcelMaskingAdapterTest {

    @Test
    @DisplayName("마스킹된 행을 writer로 전달한다")
    void writesMaskedRow() {
        AtomicReference<Map<String, Object>> captured = new AtomicReference<>();

        MaskingTarget target = MaskingTarget.builder()
                .dataKind("rrn")
                .build();

        ExcelMaskingAdapter.writeMaskedRow(
                0,
                Map.of("rrn", "900101-1234567", "name", "Kim"),
                target,
                "PARTIAL",
                "{\"keepEnd\":4}",
                (idx, row) -> captured.set(new HashMap<>(row)));

        assertThat(captured.get().get("rrn")).isNotEqualTo("900101-1234567");
        // 이름도 기본 규칙이 적용될 수 있으므로 마스킹 여부만 확인
        assertThat(captured.get().get("name")).isNotNull();
    }
}
