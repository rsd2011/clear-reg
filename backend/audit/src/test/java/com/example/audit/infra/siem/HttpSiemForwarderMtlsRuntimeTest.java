package com.example.audit.infra.siem;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;

import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.audit.AuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * mTLS 서버를 런타임에 생성해 HttpSiemForwarder가 실제 핸드셰이크를 수행하는지 검증한다.
 */
class HttpSiemForwarderMtlsRuntimeTest {

    private static HttpsServer server;
    private static volatile String lastBody;
    private static File keyStoreFile;
    private static final char[] PASSWORD = "changeit".toCharArray();

    @BeforeAll
    static void setup() throws Exception {
        KeyStore ks = buildSelfSignedKeyStore();
        keyStoreFile = File.createTempFile("siem-keystore", ".p12");
        try (FileOutputStream fos = new FileOutputStream(keyStoreFile)) {
            ks.store(fos, PASSWORD);
        }

        SSLContext ctx = SiemSslContextBuilder.build(
                keyStoreFile.getAbsolutePath(), String.valueOf(PASSWORD),
                keyStoreFile.getAbsolutePath(), String.valueOf(PASSWORD));

        server = HttpsServer.create(new InetSocketAddress(0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(ctx));
        server.createContext("/siem", new EchoHandler());
        server.start();
    }

    @AfterAll
    static void teardown() {
        if (server != null) {
            server.stop(0);
        }
        if (keyStoreFile != null) {
            keyStoreFile.delete();
        }
    }

    @Test
    @DisplayName("HttpSiemForwarder가 mTLS 서버와 성공적으로 통신한다")
    void forwardsWithMtls() {
        SiemProperties props = new SiemProperties();
        props.setEnabled(true);
        props.setEndpoint("https://localhost:" + server.getAddress().getPort() + "/siem");
        props.setKeyStore(keyStoreFile.getAbsolutePath());
        props.setKeyStorePassword(String.valueOf(PASSWORD));
        props.setTrustStore(keyStoreFile.getAbsolutePath());
        props.setTrustStorePassword(String.valueOf(PASSWORD));
        props.setHmacSecret("secret");
        props.setWhitelist(java.util.List.of("eventType"));

        HttpSiemForwarder forwarder = new HttpSiemForwarder(props, new ObjectMapper().findAndRegisterModules());

        forwarder.forward(AuditEvent.builder().eventType("MTLS_TEST").eventTime(Instant.now()).build());

        assertThat(lastBody).contains("MTLS_TEST");
    }

    private static KeyStore buildSelfSignedKeyStore() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        X509Certificate cert = SelfSignedCertGenerator.generate("CN=siem-test", kp);

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, PASSWORD);
        ks.setKeyEntry("siem", kp.getPrivate(), PASSWORD, new Certificate[]{cert});
        return ks;
    }

    static class EchoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                lastBody = new String(exchange.getRequestBody().readAllBytes());
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().close();
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
