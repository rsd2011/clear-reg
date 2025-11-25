package com.example.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.example.common.masking.MaskRuleDefinition;
import org.junit.jupiter.api.Test;

class CurrentUserTest {

    @Test
    void maskRuleLookupReturnsDefinition() {
        MaskRuleDefinition rule = new MaskRuleDefinition("TAG", "***", "UNMASK", false);
        CurrentUser user = new CurrentUser("u", "org", "perm", "FEATURE", "READ",
                RowScope.OWN, Map.of("TAG", rule));
        assertThat(user.maskRule("TAG")).contains(rule);
        assertThat(user.maskRule("UNKNOWN")).isEmpty();
    }
}
