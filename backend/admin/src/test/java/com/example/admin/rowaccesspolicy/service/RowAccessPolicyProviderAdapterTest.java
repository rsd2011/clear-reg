package com.example.admin.rowaccesspolicy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.common.policy.RowAccessMatch;
import com.example.common.policy.RowAccessQuery;
import com.example.common.security.RowScope;

@DisplayName("RowAccessPolicyProviderAdapter")
class RowAccessPolicyProviderAdapterTest {

    RowAccessPolicyService service = Mockito.mock(RowAccessPolicyService.class);
    RowAccessPolicyProviderAdapter adapter = new RowAccessPolicyProviderAdapter(service);

    @Test
    @DisplayName("evaluate가 서비스를 올바르게 호출한다")
    void evaluateDelegatesToService() {
        Instant now = Instant.now();
        RowAccessQuery query = new RowAccessQuery(
                "ORGANIZATION",
                "READ",
                "ADMIN",
                List.of("ORG_A"),
                now
        );
        RowAccessMatch expected = RowAccessMatch.builder()
                .policyId(UUID.randomUUID())
                .rowScope(RowScope.ORG)
                .priority(1)
                .build();
        given(service.evaluate(
                eq("ORGANIZATION"),
                eq("READ"),
                eq("ADMIN"),
                eq(List.of("ORG_A")),
                eq(now)
        )).willReturn(Optional.of(expected));

        Optional<RowAccessMatch> result = adapter.evaluate(query);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expected);
        verify(service).evaluate("ORGANIZATION", "READ", "ADMIN", List.of("ORG_A"), now);
    }

    @Test
    @DisplayName("서비스가 empty를 반환하면 empty를 반환한다")
    void returnsEmptyWhenServiceReturnsEmpty() {
        RowAccessQuery query = new RowAccessQuery(
                "ORGANIZATION",
                null,
                null,
                null,
                Instant.now()
        );
        given(service.evaluate(any(), any(), any(), any(), any())).willReturn(Optional.empty());

        Optional<RowAccessMatch> result = adapter.evaluate(query);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("nowOrDefault를 사용하여 시간을 전달한다")
    void usesNowOrDefault() {
        RowAccessQuery query = new RowAccessQuery(
                "ORGANIZATION",
                null,
                null,
                null,
                null  // null now
        );
        given(service.evaluate(any(), any(), any(), any(), any())).willReturn(Optional.empty());

        adapter.evaluate(query);

        // 서비스가 호출되었는지 확인
        verify(service).evaluate(eq("ORGANIZATION"), eq(null), eq(null), eq(null), any(Instant.class));
    }
}
