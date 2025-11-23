package com.example.common.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PrimaryKeyEntityTest {

    private static class DummyEntity extends PrimaryKeyEntity {}
    private static class OtherEntity extends PrimaryKeyEntity {}

    @Test
    @DisplayName("equals/hashCode 브랜치")
    void equalsAndHash() {
        DummyEntity a = new DummyEntity();
        DummyEntity b = new DummyEntity();
        assertThat(a).isEqualTo(a); // self
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo(new OtherEntity());
        assertThat(a).isNotEqualTo(b); // 다른 ID
        assertThat(a.hashCode()).isNotZero();

        // id/isNew getter & markNotNew 브랜치
        assertThat(a.getId()).isNotNull();
        assertThat(a.isNew()).isTrue();
        try {
            var m = PrimaryKeyEntity.class.getDeclaredMethod("markNotNew");
            m.setAccessible(true);
            m.invoke(a);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertThat(a.isNew()).isFalse();
    }
}
