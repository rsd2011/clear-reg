package com.example.common.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CacheInvalidationEventTest {

    @Test
    @DisplayName("CacheInvalidationType enum 값이 유지된다")
    void cacheInvalidationTypeValues() {
        assertThat(CacheInvalidationType.valueOf("ROW_SCOPE")).isEqualTo(CacheInvalidationType.ROW_SCOPE);
        assertThat(CacheInvalidationType.values()).contains(CacheInvalidationType.MASKING);
    }

    @Test
    @DisplayName("CacheInvalidationEvent는 생성자 값들을 그대로 노출한다")
    void cacheInvalidationEventStoresFields() {
        Instant now = Instant.now();
        CacheInvalidationEvent event = new CacheInvalidationEvent(CacheInvalidationType.PERMISSION_MENU, "t", "s", 1L, now);

        assertThat(event.type()).isEqualTo(CacheInvalidationType.PERMISSION_MENU);
        assertThat(event.tenantId()).isEqualTo("t");
        assertThat(event.scopeId()).isEqualTo("s");
        assertThat(event.version()).isEqualTo(1L);
        assertThat(event.issuedAt()).isEqualTo(now);
    }
}
