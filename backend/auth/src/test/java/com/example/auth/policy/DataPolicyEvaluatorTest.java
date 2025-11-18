package com.example.auth.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.FieldMaskRule;
import com.example.auth.permission.context.AuthContext;
import com.example.auth.permission.context.AuthContextHolder;
import com.example.common.security.RowScope;
import com.example.testing.bdd.Scenario;

class DataPolicyEvaluatorTest {

    private final DataPolicyEvaluator evaluator = new DataPolicyEvaluator();

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    void givenUnmaskPermission_whenMasking_thenReturnRawValue() {
        FieldMaskRule rule = new FieldMaskRule("ORG_NAME", "***", ActionCode.READ, true);
        AuthContext context = new AuthContext("user", "ORG", "AUDIT",
                FeatureCode.ORGANIZATION, ActionCode.UNMASK, RowScope.ALL, Map.of("ORG_NAME", rule));
        AuthContextHolder.set(context);

        Scenario.given("마스킹 허용", () -> evaluator.mask("ORG_NAME", "Headquarters"))
                .then("원문 노출", value -> assertThat(value).isEqualTo("Headquarters"));
    }

    @Test
    void givenMaskedContext_whenMasking_thenRedact() {
        FieldMaskRule rule = new FieldMaskRule("SALARY", "###", ActionCode.UNMASK, false);
        AuthContext context = new AuthContext("user", "ORG", "AUDIT",
                FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.OWN, Map.of("SALARY", rule));
        AuthContextHolder.set(context);

        Scenario.given("마스킹 필요", () -> evaluator.mask("SALARY", 1500000))
                .then("정책에 따라 가림", value -> assertThat(value).isEqualTo(0));
    }

    @Test
    void givenNoContext_whenMasking_thenUseDefaultPlaceholder() {
        Scenario.given("컨텍스트 없음", () -> evaluator.mask("EMAIL", "user@example.com"))
                .then("기본 마스킹", value -> assertThat(value).isEqualTo("***"));
    }
}
