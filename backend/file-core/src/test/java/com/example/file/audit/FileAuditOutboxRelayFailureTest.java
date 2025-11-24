package com.example.file.audit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.ObjectProvider;
import com.example.common.policy.PolicySettingsProvider;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileAuditOutboxRelayFailureTest {

    JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
    FileAuditPublisher publisher = Mockito.mock(FileAuditPublisher.class);
    FileAuditOutboxRelay relay = new FileAuditOutboxRelay(jdbcTemplate, publisher, 10, 5_000,
            mockProvider(), false);

    @Test
    @DisplayName("outbox update 실패 시 예외가 전파된다")
    void updateFailure_propagates() {
        given(jdbcTemplate.query(Mockito.anyString(), Mockito.<RowMapper<Object>>any(), any()))
                .willThrow(new RuntimeException("query failed"));

        assertThatThrownBy(() -> relay.relay())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("query failed");
    }

    private ObjectProvider<PolicySettingsProvider> mockProvider() {
        ObjectProvider<PolicySettingsProvider> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }
}
