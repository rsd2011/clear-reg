package com.example.common.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class PolicyToggleSettingsTest {

    @Test
    void whenLoginTypesAbsent_thenDefaultsToEmptyList() {
        PolicyToggleSettings settings = new PolicyToggleSettings(true, true, false, null);

        assertThat(settings.enabledLoginTypes()).isEmpty();
    }

    @Test
    void whenLoginTypesProvided_thenDefensiveCopyIsImmutable() {
        List<String> original = new ArrayList<>(List.of("SSO", "PASSWORD"));

        PolicyToggleSettings settings = new PolicyToggleSettings(true, true, false, original);
        original.add("AD");

        assertThat(settings.enabledLoginTypes()).containsExactly("SSO", "PASSWORD");
        assertThatThrownBy(() -> settings.enabledLoginTypes().add("NEW"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
