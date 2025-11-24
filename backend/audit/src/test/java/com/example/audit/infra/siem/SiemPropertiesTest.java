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

    @Test
    @DisplayName("equals/hashCode/toString 브랜치를 커버한다")
    void equalsHashCodeToString() {
        SiemProperties a = new SiemProperties();
        a.setEnabled(true);
        a.setEndpoint("e1");
        a.setApiKey("k1");
        a.setTimeoutMs(1);

        SiemProperties b = new SiemProperties();
        b.setEnabled(true);
        b.setEndpoint("e1");
        b.setApiKey("k1");
        b.setTimeoutMs(1);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.toString()).contains("e1").contains("k1");

        b.setEndpoint("e2");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("equals는 null/타입 불일치/필드 차이를 모두 구분한다")
    void equalsNullAndDifferentType() {
        SiemProperties base = new SiemProperties();
        base.setEnabled(true);
        base.setEndpoint("ep");
        base.setApiKey("k");
        base.setTimeoutMs(1234);

        assertThat(base.equals(null)).isFalse();
        assertThat(base.equals("string")).isFalse();

        SiemProperties different = new SiemProperties();
        different.setEnabled(false); // enabled만 다름
        different.setEndpoint("ep");
        different.setApiKey("k");
        different.setTimeoutMs(1234);
        assertThat(base).isNotEqualTo(different);

        // canEqual 방어 로직
        assertThat(base.canEqual("string")).isFalse();

        assertThat(base).isEqualTo(base); // self branch
    }

    @Test
    @DisplayName("null 필드 조합도 equals 분기를 커버한다")
    void equalsHandlesNullFields() {
        SiemProperties left = new SiemProperties();
        left.setEnabled(false);
        left.setEndpoint(null);
        left.setApiKey(null);
        left.setTimeoutMs(1000);

        SiemProperties right = new SiemProperties();
        right.setEnabled(false);
        right.setEndpoint(null);
        right.setApiKey(null);
        right.setTimeoutMs(1000);

        assertThat(left).isEqualTo(right);

        right.setApiKey("diff");
        assertThat(left).isNotEqualTo(right);
    }

    @Test
    @DisplayName("equals는 모든 필드 차이를 감지한다")
    void equalsCoversAllFields() {
        SiemProperties base = fullProps();

        String[] endpoints = {"url2"};
        // endpoint
        SiemProperties diff = fullProps();
        diff.setEndpoint("url2");
        assertThat(base).isNotEqualTo(diff);

        // apiKey
        diff = fullProps();
        diff.setApiKey("k2");
        assertThat(base).isNotEqualTo(diff);

        // timeout
        diff = fullProps();
        diff.setTimeoutMs(9999);
        assertThat(base).isNotEqualTo(diff);

        // hmacSecret
        diff = fullProps();
        diff.setHmacSecret("h2");
        assertThat(base).isNotEqualTo(diff);

        // whitelist
        diff = fullProps();
        diff.setWhitelist(java.util.List.of("b"));
        assertThat(base).isNotEqualTo(diff);

        // retry
        diff = fullProps();
        diff.setRetry(3);
        assertThat(base).isNotEqualTo(diff);

        // mode
        diff = fullProps();
        diff.setMode("syslog");
        assertThat(base).isNotEqualTo(diff);

        // keyStore
        diff = fullProps();
        diff.setKeyStore("ks2");
        assertThat(base).isNotEqualTo(diff);

        // keyStorePassword
        diff = fullProps();
        diff.setKeyStorePassword("p2");
        assertThat(base).isNotEqualTo(diff);

        // trustStore
        diff = fullProps();
        diff.setTrustStore("ts2");
        assertThat(base).isNotEqualTo(diff);

        // trustStorePassword
        diff = fullProps();
        diff.setTrustStorePassword("tp2");
        assertThat(base).isNotEqualTo(diff);

        // syslogHost
        diff = fullProps();
        diff.setSyslogHost("host2");
        assertThat(base).isNotEqualTo(diff);

        // syslogPort
        diff = fullProps();
        diff.setSyslogPort(1514);
        assertThat(base).isNotEqualTo(diff);
    }

    private SiemProperties fullProps() {
        SiemProperties p = new SiemProperties();
        p.setEnabled(true);
        p.setEndpoint("url");
        p.setApiKey("k");
        p.setTimeoutMs(1000);
        p.setHmacSecret("h");
        p.setWhitelist(java.util.List.of("a"));
        p.setRetry(2);
        p.setMode("otlp");
        p.setKeyStore("ks");
        p.setKeyStorePassword("kp");
        p.setTrustStore("ts");
        p.setTrustStorePassword("tp");
        p.setSyslogHost("host");
        p.setSyslogPort(6514);
        return p;
    }
}
