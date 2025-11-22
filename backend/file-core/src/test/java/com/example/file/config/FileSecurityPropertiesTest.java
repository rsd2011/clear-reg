package com.example.file.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FileSecurityPropertiesTest {

    @Test
    @DisplayName("기본값은 스캔 enabled=true, maxSizeBytes=0, scanTimeoutMs=10000이다")
    void defaults() {
        FileSecurityProperties props = new FileSecurityProperties();

        assertThat(props.isScanEnabled()).isTrue();
        assertThat(props.getMaxSizeBytes()).isEqualTo(0);
        assertThat(props.getScanTimeoutMs()).isEqualTo(10_000);
    }

    @Test
    @DisplayName("설정값을 세터로 변경할 수 있다")
    void settersUpdateValues() {
        FileSecurityProperties props = new FileSecurityProperties();

        props.setScanEnabled(false);
        props.setMaxSizeBytes(1234L);
        props.setScanTimeoutMs(5000L);
        props.setSignedUrlTtlSeconds(600L);
        props.setRescanEnabled(false);
        props.setRescanIntervalMs(2000L);

        assertThat(props.isScanEnabled()).isFalse();
        assertThat(props.getMaxSizeBytes()).isEqualTo(1234L);
        assertThat(props.getScanTimeoutMs()).isEqualTo(5000L);
        assertThat(props.getSignedUrlTtlSeconds()).isEqualTo(600L);
        assertThat(props.isRescanEnabled()).isFalse();
        assertThat(props.getRescanIntervalMs()).isEqualTo(2000L);
    }
}
