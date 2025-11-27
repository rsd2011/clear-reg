package com.example.dw.application.export;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LoggingExportFailureNotifier 테스트")
class LoggingExportFailureNotifierTest {

    private final LoggingExportFailureNotifier notifier = new LoggingExportFailureNotifier();

    @Test
    @DisplayName("Given ExportFailureEvent When notify 호출 Then 로그를 기록한다")
    void givenEventWhenNotifyThenLog() {
        // given
        ExportFailureEvent event = new ExportFailureEvent(
                "CSV_EXPORT",
                "test-file.csv",
                100L,
                "TIMEOUT"
        );

        // when & then - 로그 기록이므로 예외 없이 실행되면 성공
        notifier.notify(event);
    }

    @Test
    @DisplayName("Given 다양한 이벤트 데이터 When notify Then 정상 처리한다")
    void givenVariousEventDataWhenNotifyThenProcess() {
        // given
        ExportFailureEvent event = new ExportFailureEvent(
                "EXCEL_EXPORT",
                "large-data.xlsx",
                999999L,
                "DISK_FULL"
        );

        // when & then
        notifier.notify(event);
    }

    @Test
    @DisplayName("Given null 필드가 있는 이벤트 When notify Then 정상 처리한다")
    void givenNullFieldsWhenNotifyThenProcess() {
        // given
        ExportFailureEvent event = new ExportFailureEvent(
                null,
                null,
                0L,
                null
        );

        // when & then
        notifier.notify(event);
    }
}
