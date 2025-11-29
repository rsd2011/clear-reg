package com.example.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import com.example.common.policy.DataPolicyMatch;
import com.example.common.policy.RowAccessMatch;

@DisplayName("RowScopeEvaluator")
class RowScopeEvaluatorTest {

    @AfterEach
    void tearDown() {
        RowScopeContextHolder.clear();
    }

    @Nested
    @DisplayName("toSpecification(RowAccessMatch) 메서드는")
    class ToSpecificationWithRowAccessMatch {

        @Test
        @DisplayName("ctx가 제공되면 해당 컨텍스트를 사용한다")
        void usesProvidedContext() {
            RowAccessMatch match = RowAccessMatch.builder()
                    .policyId(UUID.randomUUID())
                    .rowScope(RowScope.ORG)
                    .priority(1)
                    .build();
            RowScopeContext ctx = new RowScopeContext("ORG001", List.of("ORG001"));

            Specification<Object> spec = RowScopeEvaluator.toSpecification(match, ctx, null);

            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("ctx가 null이면 RowScopeContextHolder에서 컨텍스트를 가져온다")
        void usesHolderWhenCtxNull() {
            RowScopeContextHolder.set(new RowScopeContext("ORG002", List.of("ORG002")));
            RowAccessMatch match = RowAccessMatch.builder()
                    .policyId(UUID.randomUUID())
                    .rowScope(RowScope.ALL)
                    .priority(1)
                    .build();

            Specification<Object> spec = RowScopeEvaluator.toSpecification(match, null, null);

            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("Holder에도 컨텍스트가 없고 ALL scope면 정상 동작한다")
        void handlesNullContextWithAllScope() {
            RowScopeContextHolder.clear();
            RowAccessMatch match = RowAccessMatch.builder()
                    .policyId(UUID.randomUUID())
                    .rowScope(RowScope.ALL)
                    .priority(1)
                    .build();

            Specification<Object> spec = RowScopeEvaluator.toSpecification(match, null, null);

            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("customSpec이 제공되면 함께 적용된다")
        void appliesCustomSpec() {
            RowAccessMatch match = RowAccessMatch.builder()
                    .policyId(UUID.randomUUID())
                    .rowScope(RowScope.CUSTOM)
                    .priority(1)
                    .build();
            RowScopeContext ctx = new RowScopeContext("ORG001", List.of("ORG001"));
            Specification<Object> customSpec = (root, query, cb) -> cb.equal(root.get("status"), "ACTIVE");

            Specification<Object> spec = RowScopeEvaluator.toSpecification(match, ctx, customSpec);

            assertThat(spec).isNotNull();
        }
    }

    @Nested
    @DisplayName("toSpecification(DataPolicyMatch) deprecated 메서드는")
    @SuppressWarnings("deprecation")
    class ToSpecificationWithDataPolicyMatch {

        @Test
        @DisplayName("ctx가 제공되면 해당 컨텍스트를 사용한다")
        void usesProvidedContext() {
            DataPolicyMatch match = DataPolicyMatch.builder()
                    .policyId(UUID.randomUUID())
                    .rowScope(RowScope.ORG)
                    .maskRule("NONE")
                    .priority(1)
                    .build();
            RowScopeContext ctx = new RowScopeContext("ORG001", List.of("ORG001"));

            Specification<Object> spec = RowScopeEvaluator.toSpecification(match, ctx, null);

            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("ctx가 null이면 RowScopeContextHolder에서 컨텍스트를 가져온다")
        void usesHolderWhenCtxNull() {
            RowScopeContextHolder.set(new RowScopeContext("ORG002", List.of("ORG002")));
            DataPolicyMatch match = DataPolicyMatch.builder()
                    .policyId(UUID.randomUUID())
                    .rowScope(RowScope.ALL)
                    .maskRule("PARTIAL")
                    .priority(1)
                    .build();

            Specification<Object> spec = RowScopeEvaluator.toSpecification(match, null, null);

            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("Holder에도 컨텍스트가 없고 ALL scope면 정상 동작한다")
        void handlesNullContextWithAllScope() {
            RowScopeContextHolder.clear();
            DataPolicyMatch match = DataPolicyMatch.builder()
                    .policyId(UUID.randomUUID())
                    .rowScope(RowScope.ALL)
                    .maskRule("FULL")
                    .priority(1)
                    .build();

            Specification<Object> spec = RowScopeEvaluator.toSpecification(match, null, null);

            assertThat(spec).isNotNull();
        }
    }
}
