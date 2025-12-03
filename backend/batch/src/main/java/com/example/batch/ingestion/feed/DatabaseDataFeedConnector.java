package com.example.batch.ingestion.feed;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.dw.config.DwIngestionProperties;
import com.example.dw.domain.HrExternalFeedEntity;
import com.example.dw.domain.HrExternalFeedStatus;
import com.example.dw.infrastructure.persistence.HrExternalFeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DatabaseDataFeedConnector implements DataFeedConnector {

    private final HrExternalFeedRepository externalFeedRepository;
    private final DwIngestionProperties properties;

    @Override
    @Transactional
    public Optional<DataFeed> nextFeed() {
        if (!properties.getDatabase().isEnabled()) {
            return Optional.empty();
        }
        return externalFeedRepository.findFirstByStatusOrderByCreatedAtAsc(HrExternalFeedStatus.PENDING)
                .map(this::toFeed);
    }

    private DataFeed toFeed(HrExternalFeedEntity entity) {
        entity.markProcessing();
        return new DataFeed(entity.getId().toString(),
                entity.getFeedType(),
                entity.getBusinessDate(),
                entity.getSequenceNumber(),
                entity.getPayload(),
                entity.getSourceSystem(),
                Map.of("externalFeedId", entity.getId().toString()));
    }

    @Override
    @Transactional
    public void onSuccess(DataFeed feed) {
        if (!properties.getDatabase().isEnabled()) {
            return;
        }
        externalFeedRepository.findById(UUID.fromString(feed.id()))
                .ifPresent(entity -> {
                    entity.markCompleted();
                });
    }

    @Override
    @Transactional
    public void onFailure(DataFeed feed, Exception exception) {
        if (!properties.getDatabase().isEnabled()) {
            return;
        }
        externalFeedRepository.findById(UUID.fromString(feed.id()))
                .ifPresent(entity -> {
                    log.error("External HR feed {} failed", entity.getId(), exception);
                    entity.markFailed(exception.getMessage());
                });
    }

    @Override
    public String name() {
        return "database";
    }
}
