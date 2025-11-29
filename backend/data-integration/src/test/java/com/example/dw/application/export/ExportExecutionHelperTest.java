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
class ExportExecutionHelperTest {

    ExportService exportService = Mockito.mock(ExportService.class);
    ObjectMapper objectMapper = new ObjectMapper();
    ExportExecutionHelper helper = new ExportExecutionHelper(exportService, objectMapper);

    @Test
    @DisplayName("CSV export 시 마스킹 적용 후 ExportService를 호출한다")
    void csvExportMasksAndDelegates() {
        ExportCommand cmd = new ExportCommand("csv", "masked.csv", 2, Map.of(), "RSN", "사유", "PIPA", null);
        MaskingTarget target = MaskingTarget.builder().dataKind(DataKind.ACCOUNT_NO).build();
        List<Map<String, Object>> rows = List.of(
                Map.of("account", "1234-5678-9012", "name", "Kim"),
                Map.of("account", "9876-5432-1098", "name", "Lee"),
                Map.of("name", "NoAccount")); // 헤더 없을 때도 빈 값 처리

        helper.exportCsv(cmd, rows, target, true);

        ArgumentCaptor<java.util.function.Supplier<byte[]>> captor = ArgumentCaptor.forClass(java.util.function.Supplier.class);
        verify(exportService).export(Mockito.eq(cmd), captor.capture());
        byte[] produced = captor.getValue().get();
        String csv = new String(produced, StandardCharsets.UTF_8);
        assertThat(csv).contains("account").contains("name");
        assertThat(csv).doesNotContain("1234-5678-9012");
    }

    @Test
    @DisplayName("JSON export 시 마스킹 적용 후 ExportService를 호출한다")
    void jsonExportMasksAndDelegates() {
        ExportCommand cmd = new ExportCommand("json", "masked.json", 1, Map.of(), null, null, null, null);
        MaskingTarget target = MaskingTarget.builder().dataKind(DataKind.SSN).build();
        List<Map<String, Object>> rows = List.of(Map.of("rrn", "900101-1234567"));

        helper.exportJson(cmd, rows, target, true);

        ArgumentCaptor<java.util.function.Supplier<byte[]>> captor = ArgumentCaptor.forClass(java.util.function.Supplier.class);
        verify(exportService).export(Mockito.eq(cmd), captor.capture());
        String json = new String(captor.getValue().get(), StandardCharsets.UTF_8);
        assertThat(json).doesNotContain("900101-1234567");
    }

    @Test
    @DisplayName("CSV export에서 maskingEnabled=false면 마스킹이 적용되지 않는다 (화이트리스트)")
    void csvExportWhitelist() {
        ExportCommand cmd = new ExportCommand("csv", "masked.csv", 1, Map.of(), null, null, null, null);
        MaskingTarget target = MaskingTarget.builder().dataKind(DataKind.SSN).build();
        List<Map<String, Object>> rows = List.of(Map.of("rrn", "900101-1234567"));

        helper.exportCsv(cmd, rows, target, false);

        ArgumentCaptor<java.util.function.Supplier<byte[]>> captor = ArgumentCaptor.forClass(java.util.function.Supplier.class);
        verify(exportService).export(any(), captor.capture());
        String csv = new String(captor.getAllValues().getLast().get(), StandardCharsets.UTF_8);
        // 화이트리스트: 마스킹 해제, 원본 유지
        assertThat(csv).contains("900101-1234567");
    }

    @Test
    @DisplayName("JSON export에서 maskingEnabled=true면 DataKind 기반 마스킹을 적용한다")
    void jsonExportMaskingEnabled() {
        ExportCommand cmd = new ExportCommand("json", "masked.json", 1, Map.of(), null, null, null, null);
        MaskingTarget target = MaskingTarget.builder().dataKind(DataKind.SSN).build();
        List<Map<String, Object>> rows = List.of(Map.of("rrn", "900101-1234567"));

        helper.exportJson(cmd, rows, target, true);

        ArgumentCaptor<java.util.function.Supplier<byte[]>> captor = ArgumentCaptor.forClass(java.util.function.Supplier.class);
        verify(exportService).export(any(), captor.capture());
        String json = new String(captor.getAllValues().getLast().get(), StandardCharsets.UTF_8);
        assertThat(json).doesNotContain("900101-1234567");
    }
}
