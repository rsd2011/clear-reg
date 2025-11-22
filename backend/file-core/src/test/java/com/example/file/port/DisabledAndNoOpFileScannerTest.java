package com.example.file.port;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.file.ScanStatus;

class DisabledAndNoOpFileScannerTest {

    @Test
    @DisplayName("스캔이 비활성화되면 DisabledFileScanner가 PENDING을 반환한다")
    void disabledScannerReturnsPending() {
        DisabledFileScanner scanner = new DisabledFileScanner();

        ScanStatus status = scanner.scan("dummy.txt", new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)));

        assertThat(status).isEqualTo(ScanStatus.PENDING);
    }

    @Test
    @DisplayName("스캐너 빈이 없으면 NoOpFileScanner가 CLEAN을 반환한다")
    void noOpScannerReturnsClean() {
        NoOpFileScanner scanner = new NoOpFileScanner();

        ScanStatus status = scanner.scan("dummy.txt", new ByteArrayInputStream(new byte[0]));

        assertThat(status).isEqualTo(ScanStatus.CLEAN);
    }
}
