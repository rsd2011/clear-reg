package com.example.audit.infra.siem;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SiemPropertiesTest {

    @Test
    @DisplayName("기본값을 가진다")
    void defaults() {
        SiemProperties props = new SiemProperties();
        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getTimeoutMs()).isEqualTo(3000);
    }

    @Test
    @DisplayName("세터/게터를 모두 커버한다")
    void gettersSetters() {
        SiemProperties props = new SiemProperties();
        props.setEnabled(true);
        props.setEndpoint("https://siem.test/audit");
        props.setApiKey("key");
        props.setTimeoutMs(5000);

        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getEndpoint()).isEqualTo("https://siem.test/audit");
        assertThat(props.getApiKey()).isEqualTo("key");
        assertThat(props.getTimeoutMs()).isEqualTo(5000);
    }
}
