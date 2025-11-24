package com.example.audit.infra.siem;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import com.example.audit.AuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

class HttpSiemForwarderRequestFactoryTest {

    @Test
    @DisplayName("buildRequestFactory의 익명 구현이 HTTPS 설정을 주입한다")
    void prepareConnectionSetsSsl() throws Exception {
        var ks = SiemTestUtil.writePkcs12("changeit".toCharArray());
        SiemProperties props = new SiemProperties();
        props.setEnabled(true);
        props.setEndpoint("https://localhost:0/siem");
        props.setKeyStore(ks.getAbsolutePath());
        props.setKeyStorePassword("changeit");
        props.setTrustStore(ks.getAbsolutePath());
        props.setTrustStorePassword("changeit");

        HttpSiemForwarder forwarder = new HttpSiemForwarder(props, new ObjectMapper());
        Method m = HttpSiemForwarder.class.getDeclaredMethod("buildRequestFactory");
        m.setAccessible(true);
        SimpleClientHttpRequestFactory factory = (SimpleClientHttpRequestFactory) m.invoke(forwarder);

        FakeHttpsConnection conn = new FakeHttpsConnection();
        Method pc = SimpleClientHttpRequestFactory.class.getDeclaredMethod("prepareConnection", HttpURLConnection.class, String.class);
        pc.setAccessible(true);
        pc.invoke(factory, conn, "POST");

        assertThat(conn.hostnameVerifierWasSet).isTrue();
        assertThat(conn.socketFactoryWasSet).isTrue();

        // invoke once to ensure forward still swallows exceptions
        forwarder.forward(AuditEvent.builder().eventType("REQ_FACTORY_TEST").build());
        ks.delete();
    }

    private static class FakeHttpsConnection extends HttpsURLConnection {

        boolean hostnameVerifierWasSet = false;
        boolean socketFactoryWasSet = false;

        FakeHttpsConnection() throws Exception {
            super(new URL("https://localhost"));
        }

        @Override
        public void setHostnameVerifier(javax.net.ssl.HostnameVerifier v) {
            hostnameVerifierWasSet = true;
        }

        @Override
        public void setSSLSocketFactory(javax.net.ssl.SSLSocketFactory sf) {
            socketFactoryWasSet = true;
        }

        // The remaining abstract methods are no-ops for test purposes
        @Override public void disconnect() { }
        @Override public boolean usingProxy() { return false; }
        @Override public void connect() { }
        @Override public String getCipherSuite() { return ""; }
        @Override public java.security.cert.Certificate[] getLocalCertificates() { return new java.security.cert.Certificate[0]; }
        @Override public java.security.cert.Certificate[] getServerCertificates() { return new java.security.cert.Certificate[0]; }
        @Override public java.util.Optional<javax.net.ssl.SSLSession> getSSLSession() { return java.util.Optional.empty(); }
    }
}
