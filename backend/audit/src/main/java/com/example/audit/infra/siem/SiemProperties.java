package com.example.audit.infra.siem;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "audit.siem")
@Data
public class SiemProperties {
    /**
     * SIEM 전송 활성화 여부.
     */
    private boolean enabled = false;
    /**
     * HTTP 엔드포인트 URL (예: https://siem.example.com/audit).
     */
    private String endpoint;
    /**
     * 인증용 API 키/토큰.
     */
    private String apiKey;
    /**
     * 연결/전송 타임아웃 ms.
     */
    private int timeoutMs = 3000;

    /**
     * HMAC 서명용 비밀(선택).
     */
    private String hmacSecret;

    /**
     * 전송 필드 화이트리스트(없으면 전체 전송).
     */
    private java.util.List<String> whitelist = java.util.List.of();

    /**
     * 실패 재시도 횟수.
     */
    private int retry = 1;
}
