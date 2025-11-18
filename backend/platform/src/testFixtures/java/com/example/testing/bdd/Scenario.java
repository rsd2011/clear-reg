package com.example.testing.bdd;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 간결한 BDD 스타일 테스트를 위한 헬퍼.
 */
public final class Scenario<T> {

    private final String description;
    private final T value;

    private Scenario(String description, T value) {
        this.description = Objects.requireNonNull(description, "description");
        this.value = Objects.requireNonNull(value, "value");
    }

    public static <T> Scenario<T> given(String description, Supplier<T> supplier) {
        return new Scenario<>(description, supplier.get());
    }

    public Scenario<T> and(String description, Consumer<T> consumer) {
        consumer.accept(value);
        return this;
    }

    public Scenario<T> then(String description, Consumer<T> assertion) {
        assertion.accept(value);
        return this;
    }

    public <R> ScenarioResult<R> when(String description, Function<T, R> action) {
        return new ScenarioResult<>(description, action.apply(value));
    }

    public record ScenarioResult<R>(String description, R value) {

        public ScenarioResult<R> and(String description, Consumer<R> consumer) {
            consumer.accept(value);
            return this;
        }

        public ScenarioResult<R> then(String description, Consumer<R> assertion) {
            assertion.accept(value);
            return this;
        }

        public <N> ScenarioResult<N> when(String description, Function<R, N> action) {
            return new ScenarioResult<>(description, action.apply(value));
        }
    }
}
