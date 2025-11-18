package com.example.batch.ingestion.feed;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.hr.config.HrIngestionProperties;
import com.example.hr.domain.HrExternalFeedEntity;
import com.example.hr.domain.HrExternalFeedStatus;
import com.example.hr.infrastructure.persistence.HrExternalFeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DatabaseHrFeedConnector implements HrFeedConnector {

    private final HrExternalFeedRepository externalFeedRepository;
    private final HrIngestionProperties properties;

    @Override
    @Transactional
    public Optional<HrFeed> nextFeed() {
        if (!properties.getDatabase().isEnabled()) {
            return Optional.empty();
        }
        return externalFeedRepository.findFirstByStatusOrderByCreatedAtAsc(HrExternalFeedStatus.PENDING)
                .map(this::toFeed);
    }

    private HrFeed toFeed(HrExternalFeedEntity entity) {
        entity.markProcessing();
        return new HrFeed(entity.getId().toString(),
                entity.getFeedType(),
                entity.getBusinessDate(),
                entity.getSequenceNumber(),
                entity.getPayload(),
                entity.getSourceSystem(),
                Map.of("externalFeedId", entity.getId().toString()));
    }

    @Override
    @Transactional
    public void onSuccess(HrFeed feed) {
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
    public void onFailure(HrFeed feed, Exception exception) {
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
