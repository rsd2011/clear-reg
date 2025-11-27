package com.example.audit.infra.siem;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.example.audit.AuditEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "audit.siem", name = "enabled", havingValue = "true")
@Slf4j
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring DI 주입 빈 참조")
public class HttpSiemForwarder implements SiemForwarder {

    private final SiemProperties props;
    private final ObjectMapper objectMapper;
    private RestTemplate restTemplate;

    private RestTemplate template() {
        if (restTemplate == null) {
            restTemplate = new RestTemplate(buildRequestFactory());
        }
        return restTemplate;
    }

    private SimpleClientHttpRequestFactory buildRequestFactory() {
        int timeout = props.getTimeoutMs();
        if (props.getKeyStore() != null && props.getTrustStore() != null) {
            try {
                SSLContext ctx = SiemSslContextBuilder.build(props.getKeyStore(), props.getKeyStorePassword(),
                        props.getTrustStore(), props.getTrustStorePassword());
                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
                    @Override
                    protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                        if (connection instanceof HttpsURLConnection https) {
                            https.setHostnameVerifier((h, s) -> true);
                            https.setSSLSocketFactory(ctx.getSocketFactory());
                        }
                        super.prepareConnection(connection, httpMethod);
                    }
                };
                factory.setConnectTimeout(timeout);
                factory.setReadTimeout(timeout);
                return factory;
            } catch (Exception e) {
                log.warn("Failed to build SSL context for SIEM: {}", e.getMessage());
            }
        }
        SimpleClientHttpRequestFactory fallback = new SimpleClientHttpRequestFactory();
        fallback.setConnectTimeout(timeout);
        fallback.setReadTimeout(timeout);
        return fallback;
    }

    // 테스트 주입용
    void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void forward(AuditEvent event) {
        int attempts = Math.max(1, props.getRetry());
        for (int i = 1; i <= attempts; i++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                if (props.getApiKey() != null && !props.getApiKey().isBlank()) {
                    headers.set("Authorization", "Bearer " + props.getApiKey());
                }
                Map<String, Object> payload = filter(event);
                String body = objectMapper.writeValueAsString(payload);
                if (props.getHmacSecret() != null && !props.getHmacSecret().isBlank()) {
                    headers.set("X-SIEM-SIGNATURE", hmac(body, props.getHmacSecret()));
                }
                template().postForEntity(props.getEndpoint(), new HttpEntity<>(body, headers), Void.class);
                return;
            } catch (Exception e) {
                log.warn("SIEM forward failed attempt {}/{}: {}", i, attempts, e.getMessage());
                try {
                    Thread.sleep(Math.min(120_000, (long) Math.pow(2, i) * 500L));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private Map<String, Object> filter(AuditEvent event) {
        Map<String, Object> map = objectMapper.convertValue(event, new TypeReference<Map<String, Object>>() {});
        if (props.getWhitelist() == null || props.getWhitelist().isEmpty()) {
            return map;
        }
        return map.entrySet().stream()
                .filter(e -> props.getWhitelist().contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private String hmac(String body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
