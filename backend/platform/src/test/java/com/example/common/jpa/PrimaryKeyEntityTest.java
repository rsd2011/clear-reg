package com.example.common.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.example.testing.bdd.Scenario;

class PrimaryKeyEntityTest {

    @Test
    void givenNewEntity_whenLifecycleInvoked_thenTracksPersistenceState() {
        Scenario.given("새로운 엔티티", SampleEntity::new)
                .then("초기 상태는 신규", entity -> assertThat(entity.isNew()).isTrue())
                .and("식별자가 자동 생성됨", entity -> assertThat(entity.getId()).isNotNull())
                .and("JPA lifecycle 호출 시 신규 플래그가 해제", entity -> {
                    invokeMarkNotNew(entity);
                    assertThat(entity.isNew()).isFalse();
                });
    }

    @Test
    void givenEntitiesWithSameId_whenComparing_thenShareEqualityAndHashCode() {
        UUID shared = UUID.randomUUID();
        Scenario.given("같은 ID 를 가진 엔티티", () -> pairWithSharedId(shared))
                .when("equals/hashCode 비교", pair -> pair)
                .then("동등성과 해시코드가 일치", pair -> {
                    assertThat(pair.left).isEqualTo(pair.right);
                    assertThat(pair.left.hashCode()).isEqualTo(pair.right.hashCode());
                })
                .and("ID 가 다르면 서로 다른 객체", pair -> {
                    SampleEntity other = new SampleEntity();
                    assertThat(other).isNotEqualTo(pair.left);
                });
    }

    private static void invokeMarkNotNew(PrimaryKeyEntity entity) {
        try {
            Method method = PrimaryKeyEntity.class.getDeclaredMethod("markNotNew");
            method.setAccessible(true);
            method.invoke(entity);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record EntityPair(SampleEntity left, SampleEntity right) {
    }

    private static EntityPair pairWithSharedId(UUID id) {
        SampleEntity left = new SampleEntity();
        SampleEntity right = new SampleEntity();
        setId(left, id);
        setId(right, id);
        return new EntityPair(left, right);
    }

    private static void setId(PrimaryKeyEntity entity, UUID id) {
        try {
            Field idField = PrimaryKeyEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);

            Field timestampField = PrimaryKeyEntity.class.getDeclaredField("isNew");
            timestampField.setAccessible(true);
            timestampField.set(entity, false);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class SampleEntity extends PrimaryKeyEntity {

        private Instant createdAt = Instant.now();

        Instant getCreatedAt() {
            return createdAt;
        }
    }
}
