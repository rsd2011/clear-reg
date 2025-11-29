package com.example.dw.application.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.masking.MaskingTarget;

class ExportMaskingHelperOriginalTest {

    @Test
    @DisplayName("민감필드 원문이 masked row에 남지 않는다")
    void originalNotLeaked() {
        String rrn = "900101-1234567";
        String card = "4111-1111-1111-1111";
        Map<String, Object> row = Map.of("rrn", rrn, "card", card, "name", "홍길동");

        Map<String, Object> masked = ExportMaskingHelper.maskRow(
                row,
                MaskingTarget.builder().defaultMask(true).build(),
                true);

        String concatenated = masked.values().toString();
        assertThat(concatenated).doesNotContain(rrn).doesNotContain(card);
    }
}
