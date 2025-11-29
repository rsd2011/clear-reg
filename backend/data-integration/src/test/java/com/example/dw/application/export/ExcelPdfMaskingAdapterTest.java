package com.example.dw.application.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.masking.DataKind;
import com.example.common.masking.MaskingTarget;

class ExcelPdfMaskingAdapterTest {

    @Test
    @DisplayName("ExcelMaskingAdapter가 계좌번호를 마스킹해 writer에 전달한다")
    void excelAdapterMasksAccount() {
        Map<String, Object> row = new HashMap<>();
        row.put("accountNumber", "1234-5678-9012");

        AtomicReference<Map<String, Object>> captured = new AtomicReference<>();

        ExcelMaskingAdapter.writeMaskedRow(0, row,
                MaskingTarget.builder().dataKind(DataKind.ACCOUNT_NO).build(),
                true,
                (idx, masked) -> captured.set(masked));

        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().get("accountNumber")).isNotEqualTo("1234-5678-9012");
    }

    @Test
    @DisplayName("PdfMaskingAdapter가 주민번호를 마스킹한 문자열을 전달한다")
    void pdfAdapterMasksRrn() {
        Map<String, Object> row = Map.of("rrn", "900101-1234567", "name", "홍길동");

        AtomicReference<String> captured = new AtomicReference<>();

        PdfMaskingAdapter.writeMaskedParagraph(row,
                MaskingTarget.builder().dataKind(DataKind.SSN).build(),
                true,
                captured::set);

        assertThat(captured.get()).isNotNull();
        assertThat(captured.get()).doesNotContain("900101-1234567");
    }
}
