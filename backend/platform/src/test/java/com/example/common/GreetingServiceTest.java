package com.example.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GreetingServiceTest {

    private final GreetingService greetingService = new GreetingService();

    @Test
    void whenNameProvided_thenUsesNameInGreeting() {
        assertThat(greetingService.greet("Codex"))
                .isEqualTo("Hello, Codex!");
    }

    @Test
    void whenNameMissing_thenFallsBackToWorld() {
        assertThat(greetingService.greet(null)).isEqualTo("Hello, World!");
        assertThat(greetingService.greet(" ")).isEqualTo("Hello, World!");
    }
}
