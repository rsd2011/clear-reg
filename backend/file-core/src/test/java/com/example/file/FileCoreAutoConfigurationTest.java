package com.example.file;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.example.file.audit.FileAuditPublisher;
import com.example.file.audit.NoOpFileAuditPublisher;
import com.example.file.port.FileScanner;
import com.example.file.port.NoOpFileScanner;

class FileCoreAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FileCoreAutoConfiguration.class));

    @Test
    @DisplayName("파일 스캐너 빈이 없으면 NoOpFileScanner를 자동 구성한다")
    void autoConfigProvidesNoOpScanner() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(FileScanner.class);
            assertThat(context.getBean(FileScanner.class)).isInstanceOf(NoOpFileScanner.class);
        });
    }

    @Test
    @DisplayName("파일 감사 퍼블리셔 빈이 없으면 NoOpFileAuditPublisher를 자동 구성한다")
    void autoConfigProvidesNoOpAuditPublisher() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(FileAuditPublisher.class);
            assertThat(context.getBean(FileAuditPublisher.class)).isInstanceOf(NoOpFileAuditPublisher.class);
        });
    }
}
