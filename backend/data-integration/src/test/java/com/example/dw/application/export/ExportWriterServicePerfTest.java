package com.example.dw.application.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.common.masking.MaskingTarget;
import com.example.dw.application.export.writer.ExportWriterService;

class ExportWriterServicePerfTest {

    @Test
    @DisplayName("5만 건 CSV/JSON 마스킹 처리도 OOM 없이 완료한다")
    void csvJsonLargeDataset() {
        ExportAuditService audit = Mockito.mock(ExportAuditService.class);
        ExportFailureNotifier notifier = Mockito.mock(ExportFailureNotifier.class);
        ExportService svc = new ExportService(audit, notifier);
        ExportExecutionHelper helper = new ExportExecutionHelper(svc, new com.fasterxml.jackson.databind.ObjectMapper());
        ExportWriterService writer = new ExportWriterService(svc, helper);

        List<Map<String, Object>> rows = IntStream.range(0, 50_000)
                .mapToObj(i -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("accountNumber", "1234567890123456");
                    m.put("idx", i);
                    return m;
                }).toList();

        ExportCommand cmd = new ExportCommand("csv", "mass.csv", rows.size(),
                Map.of("source", "perf"), "R", null, null, com.example.audit.AuditMode.ASYNC_FALLBACK);

        byte[] csv = helper.exportCsv(cmd, rows, MaskingTarget.builder().defaultMask(true).build(), "PARTIAL", "{\"keepEnd\":4}");
        byte[] json = helper.exportJson(cmd, rows, MaskingTarget.builder().defaultMask(true).build(), "PARTIAL", "{\"keepEnd\":4}");

        assertThat(csv.length).isGreaterThan(0);
        assertThat(json.length).isGreaterThan(0);
    }
}
