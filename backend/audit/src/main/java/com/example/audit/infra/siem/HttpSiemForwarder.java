package com.example.audit.infra.siem;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.example.audit.AuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "audit.siem", name = "enabled", havingValue = "true")
@Slf4j
public class HttpSiemForwarder implements SiemForwarder {

    private final SiemProperties props;
    private final ObjectMapper objectMapper;
    private RestTemplate restTemplate;

    private RestTemplate template() {
        if (restTemplate == null) {
            SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
            f.setConnectTimeout(props.getTimeoutMs());
            f.setReadTimeout(props.getTimeoutMs());
            restTemplate = new RestTemplate(f);
        }
        return restTemplate;
    }

    @Override
    public void forward(AuditEvent event) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (props.getApiKey() != null && !props.getApiKey().isBlank()) {
                headers.set("Authorization", "Bearer " + props.getApiKey());
            }
            String body = objectMapper.writeValueAsString(event);
            template().postForEntity(props.getEndpoint(), new HttpEntity<>(body, headers), Void.class);
        } catch (Exception e) {
            log.warn("SIEM forward failed: {}", e.getMessage());
        }
    }
}
