package com.example.dw.application.export;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.masking.MaskingTarget;

class ExportMaskingHelperNullRowTest {

    @Test
    @DisplayName("row가 null이면 빈 Map을 반환한다")
    void returnsEmptyMapWhenRowNull() {
        var target = MaskingTarget.builder().defaultMask(true).build();
        var result = ExportMaskingHelper.maskRow(null, target, null, null);
        assertThat(result).isEmpty();
    }
}
