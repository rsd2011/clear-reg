package com.example.server.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.example.common.masking.MaskingContextHolder;
import com.example.common.masking.MaskingTarget;
import com.example.dw.application.export.ExportCommand;
import com.example.dw.application.export.ExportService;

class ExportControllerTest {

    @AfterEach
    void tearDown() {
        MaskingContextHolder.clear();
    }

    @Test
    @DisplayName("ExportService를 호출하면서 마스킹을 적용한다")
    void sampleCsvMasksAndDelegates() {
        ExportService exportService = mock(ExportService.class);
        when(exportService.export(any(), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Supplier<byte[]> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        ExportController controller = new ExportController(exportService);

        MaskingContextHolder.set(MaskingTarget.builder()
                .dataKind("accountNumber")
                .forceUnmask(false)
                .build());

        var response = controller.sampleCsv("1234-5678-9012");

        String body = new String(response.getBody(), StandardCharsets.UTF_8);
        assertThat(body).contains("accountNumber");
        assertThat(body).doesNotContain("1234-5678-9012"); // 마스킹 적용됨

        ArgumentCaptor<ExportCommand> captor = ArgumentCaptor.forClass(ExportCommand.class);
        verify(exportService).export(captor.capture(), any());
        assertThat(captor.getValue().exportType()).isEqualTo("csv");
        assertThat(captor.getValue().fileName()).isEqualTo("sample.csv");
        assertThat(captor.getValue().reasonCode()).isEqualTo("RSN_SAMPLE");
        assertThat(captor.getValue().legalBasisCode()).isEqualTo("PIPA");
    }
}
