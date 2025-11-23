package com.example.dw.application.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExportConfigTest {

    @Test
    @DisplayName("ExportConfig가 LoggingExportFailureNotifier 빈을 생성한다")
    void createsNotifierBean() {
        ExportConfig config = new ExportConfig();
        ExportFailureNotifier notifier = config.exportFailureNotifier();

        assertThat(notifier).isInstanceOf(LoggingExportFailureNotifier.class);

        ExportFailureEvent event = new ExportFailureEvent("csv", "demo.csv", 10, "ERR");
        assertDoesNotThrow(() -> notifier.notify(event));
    }
}
