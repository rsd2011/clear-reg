package com.example.dw.application.export;

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

class ExportExecutionHelperTest {

    ExportService exportService = Mockito.mock(ExportService.class);
    ObjectMapper objectMapper = new ObjectMapper();
    ExportExecutionHelper helper = new ExportExecutionHelper(exportService, objectMapper);

    @Test
    @DisplayName("CSV export 시 마스킹 적용 후 ExportService를 호출한다")
    void csvExportMasksAndDelegates() {
        ExportCommand cmd = new ExportCommand("csv", "masked.csv", 2, Map.of(), "RSN", "사유", "PIPA", null);
        MaskingTarget target = MaskingTarget.builder().dataKind("accountNumber").build();
        List<Map<String, Object>> rows = List.of(
                Map.of("account", "1234-5678-9012", "name", "Kim"),
                Map.of("account", "9876-5432-1098", "name", "Lee"));

        helper.exportCsv(cmd, rows, target, "PARTIAL", "{\"keepEnd\":4}");

        ArgumentCaptor<java.util.function.Supplier<byte[]>> captor = ArgumentCaptor.forClass(java.util.function.Supplier.class);
        verify(exportService).export(Mockito.eq(cmd), captor.capture());
        byte[] produced = captor.getValue().get();
        String csv = new String(produced, StandardCharsets.UTF_8);
        assertThat(csv).contains("account,name");
        assertThat(csv).doesNotContain("1234-5678-9012");
    }

    @Test
    @DisplayName("JSON export 시 마스킹 적용 후 ExportService를 호출한다")
    void jsonExportMasksAndDelegates() {
        ExportCommand cmd = new ExportCommand("json", "masked.json", 1, Map.of(), null, null, null, null);
        MaskingTarget target = MaskingTarget.builder().dataKind("rrn").build();
        List<Map<String, Object>> rows = List.of(Map.of("rrn", "900101-1234567"));

        helper.exportJson(cmd, rows, target, "PARTIAL", null);

        ArgumentCaptor<java.util.function.Supplier<byte[]>> captor = ArgumentCaptor.forClass(java.util.function.Supplier.class);
        verify(exportService).export(Mockito.eq(cmd), captor.capture());
        String json = new String(captor.getValue().get(), StandardCharsets.UTF_8);
        assertThat(json).doesNotContain("900101-1234567");
    }
}
