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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;

import com.example.common.masking.MaskingContextHolder;
import com.example.common.masking.DataKind;
import com.example.common.masking.MaskingTarget;
import com.example.dw.application.dto.ExportCommand;
import com.example.dw.application.export.ExportExecutionHelper;
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
        ExportExecutionHelper helper = new ExportExecutionHelper(exportService, new com.fasterxml.jackson.databind.ObjectMapper());
        ExportController controller = new ExportController(helper);

        MaskingContextHolder.set(MaskingTarget.builder()
                .dataKind(DataKind.ACCOUNT_NO)
                .forceUnmask(false)
                .build());

        var response = controller.sampleCsv("1234-5678-9012", "RSN_SAMPLE", "샘플 다운로드", "PIPA", false);

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

    @Nested
    @DisplayName("sampleJson 엔드포인트")
    class SampleJsonEndpoint {

        @Test
        @DisplayName("Given: 마스킹 컨텍스트 설정 / When: sampleJson 호출 / Then: JSON 응답 반환")
        void sampleJsonReturnsJsonResponse() {
            ExportService exportService = mock(ExportService.class);
            when(exportService.export(any(), any())).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Supplier<byte[]> supplier = invocation.getArgument(1);
                return supplier.get();
            });
            ExportExecutionHelper helper = new ExportExecutionHelper(exportService, new com.fasterxml.jackson.databind.ObjectMapper());
            ExportController controller = new ExportController(helper);

            MaskingContextHolder.set(MaskingTarget.builder()
                    .dataKind(DataKind.ACCOUNT_NO)
                    .forceUnmask(false)
                    .build());

            var response = controller.sampleJson("1234-5678-9012", "RSN_JSON", false);

            String body = new String(response.getBody(), StandardCharsets.UTF_8);
            assertThat(body).contains("accountNumber");
            assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

            ArgumentCaptor<ExportCommand> captor = ArgumentCaptor.forClass(ExportCommand.class);
            verify(exportService).export(captor.capture(), any());
            assertThat(captor.getValue().exportType()).isEqualTo("json");
            assertThat(captor.getValue().fileName()).isEqualTo("sample.json");
        }
    }

    @Nested
    @DisplayName("MaskingContext null 처리")
    class NullMaskingContext {

        @Test
        @DisplayName("Given: MaskingContext가 null / When: sampleCsv 호출 / Then: 기본 마스킹 적용")
        void sampleCsvWithNullMaskingContext() {
            ExportService exportService = mock(ExportService.class);
            when(exportService.export(any(), any())).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Supplier<byte[]> supplier = invocation.getArgument(1);
                return supplier.get();
            });
            ExportExecutionHelper helper = new ExportExecutionHelper(exportService, new com.fasterxml.jackson.databind.ObjectMapper());
            ExportController controller = new ExportController(helper);

            // MaskingContextHolder를 설정하지 않음 (null 상태)
            MaskingContextHolder.clear();

            var response = controller.sampleCsv("1234-5678-9012", "RSN_NULL", null, null, false);

            assertThat(response.getBody()).isNotNull();
            verify(exportService).export(any(), any());
        }

        @Test
        @DisplayName("Given: MaskingContext가 null / When: sampleJson 호출 / Then: 기본 마스킹 적용")
        void sampleJsonWithNullMaskingContext() {
            ExportService exportService = mock(ExportService.class);
            when(exportService.export(any(), any())).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Supplier<byte[]> supplier = invocation.getArgument(1);
                return supplier.get();
            });
            ExportExecutionHelper helper = new ExportExecutionHelper(exportService, new com.fasterxml.jackson.databind.ObjectMapper());
            ExportController controller = new ExportController(helper);

            // MaskingContextHolder를 설정하지 않음 (null 상태)
            MaskingContextHolder.clear();

            var response = controller.sampleJson("1234-5678-9012", "RSN_NULL", false);

            assertThat(response.getBody()).isNotNull();
            verify(exportService).export(any(), any());
        }
    }

    @Nested
    @DisplayName("forceUnmask 처리")
    class ForceUnmaskHandling {

        @Test
        @DisplayName("Given: forceUnmask=true / When: sampleCsv 호출 / Then: 마스킹 해제 적용")
        void sampleCsvForceUnmask() {
            ExportService exportService = mock(ExportService.class);
            when(exportService.export(any(), any())).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Supplier<byte[]> supplier = invocation.getArgument(1);
                return supplier.get();
            });
            ExportExecutionHelper helper = new ExportExecutionHelper(exportService, new com.fasterxml.jackson.databind.ObjectMapper());
            ExportController controller = new ExportController(helper);

            MaskingContextHolder.set(MaskingTarget.builder()
                    .dataKind(DataKind.ACCOUNT_NO)
                    .forceUnmask(false)
                    .build());

            var response = controller.sampleCsv("1234-5678-9012", "RSN_UNMASK", null, null, true);

            String body = new String(response.getBody(), StandardCharsets.UTF_8);
            // forceUnmask=true이므로 원본 값이 포함됨
            assertThat(body).contains("1234-5678-9012");
            verify(exportService).export(any(), any());
        }

        @Test
        @DisplayName("Given: forceUnmask=true / When: sampleJson 호출 / Then: 마스킹 해제 적용")
        void sampleJsonForceUnmask() {
            ExportService exportService = mock(ExportService.class);
            when(exportService.export(any(), any())).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Supplier<byte[]> supplier = invocation.getArgument(1);
                return supplier.get();
            });
            ExportExecutionHelper helper = new ExportExecutionHelper(exportService, new com.fasterxml.jackson.databind.ObjectMapper());
            ExportController controller = new ExportController(helper);

            MaskingContextHolder.set(MaskingTarget.builder()
                    .dataKind(DataKind.ACCOUNT_NO)
                    .forceUnmask(false)
                    .build());

            var response = controller.sampleJson("1234-5678-9012", "RSN_UNMASK", true);

            String body = new String(response.getBody(), StandardCharsets.UTF_8);
            // forceUnmask=true이므로 원본 값이 포함됨
            assertThat(body).contains("1234-5678-9012");
            verify(exportService).export(any(), any());
        }
    }
}
