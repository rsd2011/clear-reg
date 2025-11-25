package com.example.auth.permission.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.FieldMaskRule;
import com.example.common.security.RowScope;
import org.junit.jupiter.api.Test;

class AuthContextCoverageTest {

    @Test
    void ruleLookupIsDefensive() {
        FieldMaskRule rule = new FieldMaskRule("TAG", "***", ActionCode.UNMASK, false);
        AuthContext ctx = new AuthContext("u", "org", "grp", FeatureCode.ORGANIZATION, ActionCode.READ,
                RowScope.ALL, Map.of("TAG", rule));
        assertThat(ctx.ruleFor("TAG")).contains(rule);
        assertThat(ctx.ruleFor("NONE")).isEmpty();
    }
}
