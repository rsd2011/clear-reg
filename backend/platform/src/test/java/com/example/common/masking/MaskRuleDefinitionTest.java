package com.example.common.masking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MaskRuleDefinitionTest {

    @Test
    void defaultsAreApplied() {
        MaskRuleDefinition def = new MaskRuleDefinition("TAG", null, null, true);
        assertThat(def.maskWith()).isEqualTo("***");
        assertThat(def.requiredActionCode()).isEqualTo("UNMASK");
        assertThat(def.audit()).isTrue();
    }

    @Test
    void tagIsRequired() {
        assertThatThrownBy(() -> new MaskRuleDefinition(" ", "***", "READ", false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
