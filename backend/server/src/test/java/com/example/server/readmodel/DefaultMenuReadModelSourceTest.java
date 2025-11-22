package com.example.server.readmodel;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DefaultMenuReadModelSourceTest {

    private final Clock fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final DefaultMenuReadModelSource source = new DefaultMenuReadModelSource(fixedClock);

    @Test
    @DisplayName("기본 메뉴 read model을 생성하고 타임스탬프와 엔트리가 포함된다")
    void snapshotCreatesDefaultMenu() {
        var model = source.snapshot();

        assertThat(model.generatedAt()).isEqualTo(OffsetDateTime.now(fixedClock));
        assertThat(model.items()).hasSize(1);
        assertThat(model.items().getFirst().code()).isEqualTo("ROOT");
    }
}
