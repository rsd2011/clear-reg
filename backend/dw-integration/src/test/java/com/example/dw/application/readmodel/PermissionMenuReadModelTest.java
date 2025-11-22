package com.example.dw.application.readmodel;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PermissionMenuReadModelTest {

    @Test
    @DisplayName("PermissionMenuReadModel은 항목과 버전을 보존한다")
    void permissionMenuReadModelStoresValues() {
        OffsetDateTime now = OffsetDateTime.parse("2024-02-01T00:00:00Z");
        PermissionMenuItem item = new PermissionMenuItem("PCODE", "권한", "FEATURE", "ACTION", "/path", Set.of("ADMIN"));

        PermissionMenuReadModel model = new PermissionMenuReadModel("pv1", now, List.of(item));

        assertThat(model.version()).isEqualTo("pv1");
        assertThat(model.generatedAt()).isEqualTo(now);
        assertThat(model.items()).containsExactly(item);
    }
}
