package com.example.server.readmodel;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DefaultPermissionMenuReadModelSourceTest {

    private final Clock fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final DefaultPermissionMenuReadModelSource source = new DefaultPermissionMenuReadModelSource(fixedClock);

    @Test
    @DisplayName("기본 권한 메뉴 read model을 생성하고 principalId에 관계없이 항목을 포함한다")
    void snapshotCreatesDefaultPermissionMenu() {
        var model = source.snapshot("user-1");

        assertThat(model.generatedAt()).isEqualTo(OffsetDateTime.now(fixedClock));
        assertThat(model.items()).hasSize(1);
        assertThat(model.items().getFirst().code()).isEqualTo("PM_ROOT");
    }
}
