package com.example.draft.application.audit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.ObjectProvider;
import com.example.common.policy.PolicySettingsProvider;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class OutboxDraftAuditRelayEmptyTest {

    @Test
    @DisplayName("Outbox가 비어 있으면 update를 호출하지 않는다")
    void noUpdateWhenEmpty() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        given(jdbc.queryForList(org.mockito.ArgumentMatchers.anyString())).willReturn(List.of());

        OutboxDraftAuditRelay relay = new OutboxDraftAuditRelay(jdbc, 60_000, nullProvider(), false);
        relay.relay();

        verify(jdbc, never()).update(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.<Object[]>any()
        );

        org.assertj.core.api.Assertions.assertThat(relay.trigger()).isNotNull();
    }

    @Test
    @DisplayName("정책 스케줄이 있으면 그것을 사용한다")
    void triggerUsesPolicySchedule() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ObjectProvider<PolicySettingsProvider> provider = mock(ObjectProvider.class);
        PolicySettingsProvider policy = mock(PolicySettingsProvider.class);
        when(provider.getIfAvailable()).thenReturn(policy);
        when(policy.batchJobSchedule(com.example.common.schedule.BatchJobCode.DRAFT_AUDIT_OUTBOX_RELAY))
                .thenReturn(new com.example.common.schedule.BatchJobSchedule(true, com.example.common.schedule.TriggerType.FIXED_DELAY, null, 7777, 0, null));

        OutboxDraftAuditRelay relay = new OutboxDraftAuditRelay(jdbc, 60_000, provider, false);

        org.assertj.core.api.Assertions.assertThat(relay.trigger().toString()).contains("7777");
    }

    private ObjectProvider<PolicySettingsProvider> nullProvider() {
        ObjectProvider<PolicySettingsProvider> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }
}
