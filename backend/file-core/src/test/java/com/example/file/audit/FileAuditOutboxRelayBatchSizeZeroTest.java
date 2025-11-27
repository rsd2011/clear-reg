package com.example.file.audit;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.ObjectProvider;
import com.example.common.policy.PolicySettingsProvider;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class FileAuditOutboxRelayBatchSizeZeroTest {

    @Test
    @DisplayName("batchSize가 0이면 쿼리 없이 종료한다")
    void relayReturnsImmediatelyWhenBatchSizeZero() {
        JdbcTemplate jdbc = Mockito.mock(JdbcTemplate.class);
        FileAuditPublisher publisher = Mockito.mock(FileAuditPublisher.class);

        FileAuditOutboxRelay relay = new FileAuditOutboxRelay(jdbc, publisher, 0, 5_000, nullProvider(), false);

        assertThatCode(relay::relay).doesNotThrowAnyException();
    }

    private ObjectProvider<PolicySettingsProvider> nullProvider() {
        ObjectProvider<PolicySettingsProvider> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }
}
