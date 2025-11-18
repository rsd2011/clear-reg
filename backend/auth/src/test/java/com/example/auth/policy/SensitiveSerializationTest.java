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
import com.fasterxml.jackson.databind.ObjectMapper;

class SensitiveSerializationTest {

    private final DataPolicyEvaluator evaluator = new DataPolicyEvaluator();

    @AfterEach
    void cleanup() {
        AuthContextHolder.clear();
    }

    @Test
    void givenSensitiveField_whenSerializing_thenMaskUnlessAllowed() throws Exception {
        FieldMaskRule rule = new FieldMaskRule("SECRET", "MASKED", ActionCode.UNMASK, true);
        AuthContextHolder.set(new AuthContext("auditor", "ORG", "AUDIT",
                FeatureCode.ORGANIZATION, ActionCode.UNMASK, RowScope.ALL, Map.of("SECRET", rule)));

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new SensitiveDataMaskingModule(evaluator));

        SensitiveBean payload = new SensitiveBean("token", "visible");
        String json = mapper.writeValueAsString(payload);
        assertThat(json).contains("\"secret\":\"token\"");
        assertThat(json).contains("\"plain\":\"visible\"");
    }

    @Test
    void givenReadOnlyContext_whenSerializing_thenMaskSecret() throws Exception {
        FieldMaskRule rule = new FieldMaskRule("SECRET", "MASKED", ActionCode.UNMASK, true);
        AuthContextHolder.set(new AuthContext("auditor", "ORG", "AUDIT",
                FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ALL, Map.of("SECRET", rule)));

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new SensitiveDataMaskingModule(evaluator));

        SensitiveBean payload = new SensitiveBean("token", "visible");
        String json = mapper.writeValueAsString(payload);
        assertThat(json).contains("\"secret\":\"MASKED\"");
    }

    private static final class SensitiveBean {

        @Sensitive("SECRET")
        private final String secret;
        private final String plain;

        private SensitiveBean(String secret, String plain) {
            this.secret = secret;
            this.plain = plain;
        }

        public String getSecret() {
            return secret;
        }

        public String getPlain() {
            return plain;
        }
    }

    @Test
    void givenConfiguration_whenCreatingModule_thenReusable() {
        DataPolicyConfiguration configuration = new DataPolicyConfiguration();
        SensitiveDataMaskingModule module = configuration.sensitiveDataMaskingModule(new DataPolicyEvaluator());
        assertThat(module).isNotNull();
    }
}
