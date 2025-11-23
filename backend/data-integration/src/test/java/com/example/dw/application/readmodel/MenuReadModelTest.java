package com.example.dw.application.readmodel;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MenuReadModelTest {

    @Test
    @DisplayName("MenuReadModel은 생성한 값들을 그대로 보존한다")
    void menuReadModelStoresValues() {
        OffsetDateTime now = OffsetDateTime.parse("2024-02-01T00:00:00Z");
        MenuItem item = new MenuItem("CODE", "이름", "FEATURE", "ACTION", "/path");

        MenuReadModel model = new MenuReadModel("v1", now, List.of(item));

        assertThat(model.version()).isEqualTo("v1");
        assertThat(model.generatedAt()).isEqualTo(now);
        assertThat(model.items()).containsExactly(item);
    }
}
