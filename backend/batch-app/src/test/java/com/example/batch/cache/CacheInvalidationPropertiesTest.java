package com.example.batch.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CacheInvalidationPropertiesTest {

    @Test
    @DisplayName("기본값은 비활성화이며 채널 이름은 cache-invalidation이다")
    void hasReasonableDefaults() {
        CacheInvalidationProperties properties = new CacheInvalidationProperties();

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getChannel()).isEqualTo("cache-invalidation");
    }

    @Test
    @DisplayName("채널 이름과 활성화 여부를 변경할 수 있다")
    void canOverrideProperties() {
        CacheInvalidationProperties properties = new CacheInvalidationProperties();

        properties.setEnabled(true);
        properties.setChannel("custom-channel");

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getChannel()).isEqualTo("custom-channel");
    }
}
