package com.example.file.audit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.ObjectProvider;
import com.example.common.policy.PolicySettingsProvider;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileAuditOutboxRelayExceptionTest {

    @Test
    @DisplayName("쿼리 단계에서 예외가 발생하면 그대로 전파한다")
    void relayPropagatesQueryException() {
        JdbcTemplate jdbc = Mockito.mock(JdbcTemplate.class);
        FileAuditPublisher publisher = Mockito.mock(FileAuditPublisher.class);
        when(jdbc.query(Mockito.anyString(), Mockito.<org.springframework.jdbc.core.RowMapper<?>>any(), Mockito.anyInt()))
                .thenThrow(new IllegalStateException("db down"));

        FileAuditOutboxRelay relay = new FileAuditOutboxRelay(jdbc, publisher, 50, 5_000, nullProvider(), false);

        assertThatThrownBy(relay::relay)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("db down");
    }

    private ObjectProvider<PolicySettingsProvider> nullProvider() {
        ObjectProvider<PolicySettingsProvider> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }
}
