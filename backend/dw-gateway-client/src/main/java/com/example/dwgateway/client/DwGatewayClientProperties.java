package com.example.dwgateway.client;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("dw.gateway.client")
public class DwGatewayClientProperties {

    /** Base URI for the DW Gateway service. */
    private URI baseUri = URI.create("http://localhost:8081");

    /** Toggle to enable/disable the port client auto-configuration. */
    private boolean enabled = true;

    private Duration connectTimeout = Duration.ofSeconds(3);

    private Duration readTimeout = Duration.ofSeconds(10);

    /** Optional service-to-service authentication token header/value. */
    private String serviceToken;

    private String serviceTokenHeader = "X-Service-Token";

    private final Retry retry = new Retry();

    public URI getBaseUri() {
        return baseUri;
    }

    public void setBaseUri(URI baseUri) {
        this.baseUri = baseUri;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public String getServiceToken() {
        return serviceToken;
    }

    public void setServiceToken(String serviceToken) {
        this.serviceToken = serviceToken;
    }

    public String getServiceTokenHeader() {
        return serviceTokenHeader;
    }

    public void setServiceTokenHeader(String serviceTokenHeader) {
        this.serviceTokenHeader = serviceTokenHeader;
    }

    public Retry getRetry() {
        return retry;
    }

    public static class Retry {
        private int maxAttempts = 3;
        private Duration backoff = Duration.ofMillis(200);

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getBackoff() {
            return backoff;
        }

        public void setBackoff(Duration backoff) {
            this.backoff = backoff;
        }
    }
}
