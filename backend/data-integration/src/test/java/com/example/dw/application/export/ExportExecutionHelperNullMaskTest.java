package com.example.dw.application.export;

import com.example.dw.application.dto.ExportCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.example.common.masking.MaskingTarget;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("unchecked")
class ExportExecutionHelperNullMaskTest {

    ExportService exportService = Mockito.mock(ExportService.class);
    ObjectMapper objectMapper = new ObjectMapper();
    ExportExecutionHelper helper = new ExportExecutionHelper(exportService, objectMapper);

    @Test
    @DisplayName("maskRule가 null이고 maskParams도 없으면 기본 PARTIAL 마스킹을 적용한다 (CSV)")
    void csvMaskRuleNull() {
        ExportCommand cmd = new ExportCommand("csv", "masked.csv", 1, Map.of(), null, null, null, null);
        List<Map<String, Object>> rows = List.of(Map.of("rrn", "900101-1234567"));

        helper.exportCsv(cmd, rows, MaskingTarget.builder().dataKind("rrn").build(), null, "");

        ArgumentCaptor<java.util.function.Supplier<byte[]>> captor = ArgumentCaptor.forClass(java.util.function.Supplier.class);
        verify(exportService).export(any(), captor.capture());
        String csv = new String(captor.getValue().get(), StandardCharsets.UTF_8);
        assertThat(csv).doesNotContain("900101-1234567");
    }

    @Test
    @DisplayName("maskRule가 null이면 JSON도 기본 PARTIAL 마스킹을 적용한다")
    void jsonMaskRuleNull() {
        ExportCommand cmd = new ExportCommand("json", "masked.json", 1, Map.of(), null, null, null, null);
        List<Map<String, Object>> rows = List.of(Map.of("account", "1234-5678-9012"));

        helper.exportJson(cmd, rows, MaskingTarget.builder().dataKind("account").build(), null, null);

        ArgumentCaptor<java.util.function.Supplier<byte[]>> captor = ArgumentCaptor.forClass(java.util.function.Supplier.class);
        verify(exportService).export(any(), captor.capture());
        String json = new String(captor.getValue().get(), StandardCharsets.UTF_8);
        assertThat(json).doesNotContain("1234-5678-9012");
    }
}
