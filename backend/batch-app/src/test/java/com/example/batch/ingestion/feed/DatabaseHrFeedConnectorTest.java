package com.example.batch.ingestion.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.hr.config.HrIngestionProperties;
import com.example.hr.domain.HrExternalFeedEntity;
import com.example.hr.domain.HrExternalFeedStatus;
import com.example.hr.dto.HrFeedType;
import com.example.hr.infrastructure.persistence.HrExternalFeedRepository;

@ExtendWith(MockitoExtension.class)
class DatabaseHrFeedConnectorTest {

    @Mock
    private HrExternalFeedRepository repository;

    private HrIngestionProperties properties;
    private DatabaseHrFeedConnector connector;

    @BeforeEach
    void setUp() {
        properties = new HrIngestionProperties();
        properties.getDatabase().setEnabled(true);
        connector = new DatabaseHrFeedConnector(repository, properties);
    }

    @Test
    void givenEnabledDatabase_whenFetching_thenReturnFeedAndMarkProcessing() {
        HrExternalFeedEntity entity = new HrExternalFeedEntity();
        entity.setFeedType(HrFeedType.EMPLOYEE);
        entity.setPayload("payload");
        entity.setBusinessDate(LocalDate.now());
        entity.setSequenceNumber(4);
        entity.setSourceSystem("db");
        given(repository.findFirstByStatusOrderByCreatedAtAsc(HrExternalFeedStatus.PENDING))
                .willReturn(Optional.of(entity));

        Optional<HrFeed> feed = connector.nextFeed();

        assertThat(feed).isPresent();
        assertThat(feed.get().feedType()).isEqualTo(HrFeedType.EMPLOYEE);
        assertThat(entity.getStatus()).isEqualTo(HrExternalFeedStatus.PROCESSING);
    }

    @Test
    void givenDisabledDatabase_whenFetching_thenSkipRepository() {
        properties.getDatabase().setEnabled(false);
        DatabaseHrFeedConnector disabledConnector = new DatabaseHrFeedConnector(repository, properties);

        assertThat(disabledConnector.nextFeed()).isEmpty();
    }

    @Test
    void givenFeedSuccess_whenOnSuccess_thenMarkCompleted() {
        HrExternalFeedEntity entity = new HrExternalFeedEntity();
        UUID id = entity.getId();
        HrFeed feed = new HrFeed(id.toString(), HrFeedType.EMPLOYEE, LocalDate.now(), 1, "payload", "db", java.util.Map.of());
        given(repository.findById(id)).willReturn(Optional.of(entity));

        connector.onSuccess(feed);

        assertThat(entity.getStatus()).isEqualTo(HrExternalFeedStatus.COMPLETED);
        verify(repository).findById(id);
    }

    @Test
    void givenFeedFailure_whenOnFailure_thenMarkFailed() {
        HrExternalFeedEntity entity = new HrExternalFeedEntity();
        UUID id = entity.getId();
        HrFeed feed = new HrFeed(id.toString(), HrFeedType.EMPLOYEE, LocalDate.now(), 1, "payload", "db", java.util.Map.of());
        given(repository.findById(id)).willReturn(Optional.of(entity));

        RuntimeException error = new RuntimeException("boom");
        connector.onFailure(feed, error);

        assertThat(entity.getStatus()).isEqualTo(HrExternalFeedStatus.FAILED);
        assertThat(entity.getErrorMessage()).isEqualTo("boom");
    }
}
