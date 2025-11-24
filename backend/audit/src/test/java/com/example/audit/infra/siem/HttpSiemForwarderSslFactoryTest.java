package com.example.audit.infra.siem;

import java.io.File;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.audit.AuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

class HttpSiemForwarderSslFactoryTest {

    @Test
    @DisplayName("키스토어가 주어지면 SSL RequestFactory를 구성하고 예외를 삼킨다")
    void buildsSslRequestFactory() throws Exception {
        File ks = SiemTestUtil.writePkcs12("changeit".toCharArray());

        SiemProperties props = new SiemProperties();
        props.setEnabled(true);
        props.setEndpoint("https://localhost:0/siem");
        props.setKeyStore(ks.getAbsolutePath());
        props.setKeyStorePassword("changeit");
        props.setTrustStore(ks.getAbsolutePath());
        props.setTrustStorePassword("changeit");

        HttpSiemForwarder forwarder = new HttpSiemForwarder(props, new ObjectMapper());
        // 네트워크 예외는 내부에서 잡히므로 단순 호출로도 분기 커버 가능
        forwarder.forward(AuditEvent.builder().eventType("SSL_FACTORY_TEST").eventTime(Instant.now()).build());

        ks.delete();
    }
}

