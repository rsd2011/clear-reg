package com.example.dw.application.export;

import com.example.dw.application.dto.ExportCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.common.masking.MaskingTarget;
import com.fasterxml.jackson.databind.ObjectMapper;

class ExportExecutionHelperEmptyTest {

    ExportService exportService = Mockito.mock(ExportService.class);
    ObjectMapper objectMapper = new ObjectMapper();
    ExportExecutionHelper helper = new ExportExecutionHelper(exportService, objectMapper);

    ExportExecutionHelperEmptyTest() {
        Mockito.lenient().when(exportService.export(any(), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Supplier<byte[]> supplier = invocation.getArgument(1);
            return supplier.get();
        });
    }

    @Test
    @DisplayName("CSV export는 빈 rows면 빈 byte 배열을 반환하고 ExportService를 호출한다")
    void csvExportEmptyRows() {
        byte[] result = helper.exportCsv(new ExportCommand("csv", "empty.csv", 0, Map.of(), null, null, null, null),
                List.of(), MaskingTarget.builder().build(), true);

        verify(exportService).export(any(), any());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("JSON export는 빈 rows면 빈 배열 JSON을 반환한다")
    void jsonExportEmptyRows() {
        byte[] result = helper.exportJson(new ExportCommand("json", "empty.json", 0, Map.of(), null, null, null, null),
                List.of(), MaskingTarget.builder().build(), true);

        assertThat(new String(result).replaceAll("\\s", "")).isEqualTo("[]");
    }
}
