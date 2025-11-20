package com.example.batch.ingestion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.domain.repository.HrBatchRepository;
import com.example.dw.dto.DataFeedType;
import com.example.batch.ingestion.feed.DataFeed;
import com.example.batch.ingestion.feed.DataFeedConnector;
import com.example.batch.ingestion.template.DwFeedIngestionTemplate;
import com.example.batch.ingestion.template.DwIngestionResult;

@Service
@Slf4j
public class DwIngestionService {
    private static final HexFormat HEX = HexFormat.of();

    private final List<DataFeedConnector> feedConnectors;
    private final Map<DataFeedType, DwFeedIngestionTemplate> templateRegistry;
    private final HrBatchRepository batchRepository;

    @Transactional
    public Optional<HrImportBatchEntity> ingestNextFile() {
        for (DataFeedConnector connector : feedConnectors) {
            Optional<DataFeed> feedOpt = connector.nextFeed();
            if (feedOpt.isEmpty()) {
                continue;
            }
            DataFeed feed = feedOpt.get();
            HrImportBatchEntity batch = createBatch(feed);
            try {
                DwFeedIngestionTemplate template = templateRegistry.get(feed.feedType());
                if (template == null) {
                    connector.onFailure(feed, new IllegalStateException("지원되지 않는 피드 타입"));
                    batch.markFailed("Unsupported feed type " + feed.feedType());
                    batchRepository.save(batch);
                    continue;
                }
                DwIngestionResult result = template.ingest(batch, feed.payload());
                batch.markValidated(result.totalRecords(), result.failedRecords());
                batch.markCompleted(result.insertedRecords(), result.updatedRecords(), result.failedRecords());
                batchRepository.save(batch);
                connector.onSuccess(feed);
                log.info("DW batch {} processed from {}", batch.getId(), feed.source());
                return Optional.of(batch);
            } catch (Exception ex) {
                connector.onFailure(feed, ex);
                log.error("Failed to process DW batch from {}", feed.source(), ex);
                batch.markFailed(ex.getMessage());
                batchRepository.save(batch);
                return Optional.of(batch);
            }
        }
        return Optional.empty();
    }

    private HrImportBatchEntity createBatch(DataFeed feed) {
        HrImportBatchEntity batch = new HrImportBatchEntity();
        batch.setFileName(feed.id());
        batch.setFeedType(feed.feedType());
        batch.setSourceName(feed.source());
        batch.setBusinessDate(feed.businessDate());
        batch.setSequenceNumber(feed.sequenceNumber());
        batch.setChecksum(hash(feed.payload()));
        batch.setSourcePath(feed.source());
        batch.setReceivedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return batchRepository.save(batch);
    }

    private static String hash(String payload) {
        if (payload == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not supported", ex);
        }
    }

    public DwIngestionService(List<DataFeedConnector> feedConnectors,
                              List<DwFeedIngestionTemplate> ingestionTemplates,
                              HrBatchRepository batchRepository) {
        this.feedConnectors = feedConnectors;
        this.batchRepository = batchRepository;
        this.templateRegistry = ingestionTemplates.stream()
                .collect(Collectors.toUnmodifiableMap(DwFeedIngestionTemplate::supportedType, template -> template));
    }
}
