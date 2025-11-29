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

import com.example.common.masking.DataKind;
import com.example.common.masking.MaskingTarget;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("unchecked")
class ExportExecutionHelperNullMaskTest {

    ExportService exportService = Mockito.mock(ExportService.class);
    ObjectMapper objectMapper = new ObjectMapper();
    ExportExecutionHelper helper = new ExportExecutionHelper(exportService, objectMapper);

    @Test
    @DisplayName("maskingEnabled=true면 DataKind 기반 마스킹을 적용한다 (CSV)")
    void csvMaskingEnabled() {
        ExportCommand cmd = new ExportCommand("csv", "masked.csv", 1, Map.of(), null, null, null, null);
        List<Map<String, Object>> rows = List.of(Map.of("rrn", "900101-1234567"));

        helper.exportCsv(cmd, rows, MaskingTarget.builder().dataKind(DataKind.SSN).build(), true);

        ArgumentCaptor<java.util.function.Supplier<byte[]>> captor = ArgumentCaptor.forClass(java.util.function.Supplier.class);
        verify(exportService).export(any(), captor.capture());
        String csv = new String(captor.getValue().get(), StandardCharsets.UTF_8);
        assertThat(csv).doesNotContain("900101-1234567");
    }

    @Test
    @DisplayName("maskingEnabled=true면 JSON도 DataKind 기반 마스킹을 적용한다")
    void jsonMaskingEnabled() {
        ExportCommand cmd = new ExportCommand("json", "masked.json", 1, Map.of(), null, null, null, null);
        List<Map<String, Object>> rows = List.of(Map.of("account", "1234-5678-9012"));

        helper.exportJson(cmd, rows, MaskingTarget.builder().dataKind(DataKind.ACCOUNT_NO).build(), true);

        ArgumentCaptor<java.util.function.Supplier<byte[]>> captor = ArgumentCaptor.forClass(java.util.function.Supplier.class);
        verify(exportService).export(any(), captor.capture());
        String json = new String(captor.getValue().get(), StandardCharsets.UTF_8);
        assertThat(json).doesNotContain("1234-5678-9012");
    }
}
