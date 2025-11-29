package com.example.admin.maskingpolicy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.common.masking.DataKind;
import com.example.common.policy.MaskingMatch;
import com.example.common.policy.MaskingQuery;

@DisplayName("MaskingPolicyProviderAdapter")
class MaskingPolicyProviderAdapterTest {

    MaskingPolicyService service = Mockito.mock(MaskingPolicyService.class);
    MaskingPolicyProviderAdapter adapter = new MaskingPolicyProviderAdapter(service);

    @Test
    @DisplayName("evaluate가 서비스를 올바르게 호출한다")
    void evaluateDelegatesToService() {
        Instant now = Instant.now();
        MaskingQuery query = new MaskingQuery(
                "ORGANIZATION",
                "READ",
                "ADMIN",
                List.of("ORG_A"),
                DataKind.SSN,
                now
        );
        MaskingMatch expected = MaskingMatch.builder()
                .policyId(UUID.randomUUID())
                .dataKinds(java.util.Set.of(DataKind.SSN))
                .maskingEnabled(true)
                .auditEnabled(true)
                .priority(1)
                .build();
        given(service.evaluate(
                eq("ORGANIZATION"),
                eq("READ"),
                eq("ADMIN"),
                eq(List.of("ORG_A")),
                eq(DataKind.SSN),
                eq(now)
        )).willReturn(Optional.of(expected));

        Optional<MaskingMatch> result = adapter.evaluate(query);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expected);
        verify(service).evaluate("ORGANIZATION", "READ", "ADMIN", List.of("ORG_A"), DataKind.SSN, now);
    }

    @Test
    @DisplayName("서비스가 empty를 반환하면 empty를 반환한다")
    void returnsEmptyWhenServiceReturnsEmpty() {
        Instant now = Instant.now();
        MaskingQuery query = new MaskingQuery(
                "ORGANIZATION",
                null,
                null,
                null,
                (DataKind) null,
                now
        );
        given(service.evaluate(eq("ORGANIZATION"), isNull(), isNull(), isNull(), isNull(DataKind.class), eq(now)))
                .willReturn(Optional.empty());

        Optional<MaskingMatch> result = adapter.evaluate(query);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("nowOrDefault를 사용하여 시간을 전달한다")
    void usesNowOrDefault() {
        MaskingQuery query = new MaskingQuery(
                "ORGANIZATION",
                null,
                null,
                null,
                DataKind.PHONE,
                null  // null now
        );
        given(service.evaluate(eq("ORGANIZATION"), isNull(), isNull(), isNull(), eq(DataKind.PHONE), any(Instant.class)))
                .willReturn(Optional.empty());

        adapter.evaluate(query);

        // 서비스가 호출되었는지 확인
        verify(service).evaluate(eq("ORGANIZATION"), isNull(), isNull(), isNull(), eq(DataKind.PHONE), any(Instant.class));
    }
}
