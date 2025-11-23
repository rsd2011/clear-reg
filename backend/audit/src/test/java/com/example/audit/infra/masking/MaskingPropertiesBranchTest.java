package com.example.audit.infra.masking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MaskingProperties setRules 분기 커버")
class MaskingPropertiesBranchTest {

    @Test
    void setRulesNullOrEmptyKeepsDefaults() {
        MaskingProperties props = new MaskingProperties();
        int defaultSize = props.getRules().size();
        props.setRules(null);
        assertThat(props.getRules()).hasSize(defaultSize);
        props.setRules(java.util.List.of());
        assertThat(props.getRules()).hasSize(defaultSize);
    }
}

