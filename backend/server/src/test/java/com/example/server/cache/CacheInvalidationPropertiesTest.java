package com.example.server.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CacheInvalidationPropertiesTest {

    @Test
    @DisplayName("기본값은 enabled=true, channel=cache-invalidation")
    void defaults() {
        CacheInvalidationProperties props = new CacheInvalidationProperties();

        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getChannel()).isEqualTo("cache-invalidation");
    }

    @Test
    @DisplayName("세터로 값을 변경할 수 있다")
    void setters() {
        CacheInvalidationProperties props = new CacheInvalidationProperties();

        props.setEnabled(false);
        props.setChannel("chan");

        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getChannel()).isEqualTo("chan");
    }
}

