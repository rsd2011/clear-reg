package com.example.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "file.security")
@Getter
@Setter
public class FileSecurityProperties {

    /**
     * 전역 스캔 활성화 여부. 기본 true.
     */
    private boolean scanEnabled = true;

    /**
     * 스캔/업로드 허용 최대 크기(bytes). 기본 0 = 무제한(정책 별도 적용).
     */
    private long maxSizeBytes = 0;

    /**
     * 스캔 타임아웃(ms). 기본 10초.
     */
    private long scanTimeoutMs = 10_000;

    /**
     * 서명 URL TTL(초). 기본 300초.
     */
    private long signedUrlTtlSeconds = 300;

    /**
    * 스캔 재시도 활성화 여부.
    */
    private boolean rescanEnabled = true;

    /**
    * 재시도 주기(ms). 기본 60초.
    */
    private long rescanIntervalMs = 60_000;

}
