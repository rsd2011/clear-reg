package com.example.batch.ingestion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.example.hr.domain.HrImportBatchEntity;
import com.example.hr.domain.repository.HrBatchRepository;
import com.example.hr.dto.HrFeedType;
import com.example.hr.dto.HrOrganizationValidationResult;
import com.example.hr.dto.HrSyncResult;
import com.example.hr.dto.HrValidationResult;
import com.example.batch.ingestion.feed.HrFeed;
import com.example.batch.ingestion.feed.HrFeedConnector;

@Service
@RequiredArgsConstructor
@Slf4j
public class HrIngestionService {
    private static final HexFormat HEX = HexFormat.of();

    private final List<HrFeedConnector> feedConnectors;
    private final HrCsvRecordParser parser;
    private final HrRecordValidator validator;
    private final HrStagingService stagingService;
    private final HrEmployeeSynchronizationService synchronizationService;
    private final HrOrganizationCsvParser organizationCsvParser;
    private final HrOrganizationValidator organizationValidator;
    private final HrOrganizationStagingService organizationStagingService;
    private final HrOrganizationSynchronizationService organizationSynchronizationService;
    private final HrBatchRepository batchRepository;

    @Transactional
    public Optional<HrImportBatchEntity> ingestNextFile() {
        for (HrFeedConnector connector : feedConnectors) {
            Optional<HrFeed> feedOpt = connector.nextFeed();
            if (feedOpt.isEmpty()) {
                continue;
            }
            HrFeed feed = feedOpt.get();
            HrImportBatchEntity batch = createBatch(feed);
            try {
                BatchProcessingResult result = switch (feed.feedType()) {
                    case EMPLOYEE -> processEmployeePayload(batch, feed.payload());
                    case ORGANIZATION -> processOrganizationPayload(batch, feed.payload());
                };
                batch.markValidated(result.totalRecords(), result.failedRecords());
                batch.markCompleted(result.insertedRecords(), result.updatedRecords(), result.failedRecords());
                batchRepository.save(batch);
                connector.onSuccess(feed);
                log.info("HR batch {} processed from {}", batch.getId(), feed.source());
                return Optional.of(batch);
            } catch (Exception ex) {
                connector.onFailure(feed, ex);
                log.error("Failed to process HR batch from {}", feed.source(), ex);
                batch.markFailed(ex.getMessage());
                batchRepository.save(batch);
                return Optional.of(batch);
            }
        }
        return Optional.empty();
    }

    private HrImportBatchEntity createBatch(HrFeed feed) {
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

    private BatchProcessingResult processEmployeePayload(HrImportBatchEntity batch, String payload) {
        List<com.example.hr.dto.HrEmployeeRecord> parsed = parser.parse(payload);
        HrValidationResult validation = validator.validate(parsed);
        stagingService.persistRecords(batch, validation.validRecords());
        stagingService.persistErrors(batch, validation.errors());
        HrSyncResult syncResult = synchronizationService.synchronize(batch, validation.validRecords());
        return new BatchProcessingResult(parsed.size(), syncResult.insertedRecords(), syncResult.updatedRecords(),
                validation.errors().size());
    }

    private BatchProcessingResult processOrganizationPayload(HrImportBatchEntity batch, String payload) {
        List<com.example.hr.dto.HrOrganizationRecord> parsed = organizationCsvParser.parse(payload);
        HrOrganizationValidationResult validation = organizationValidator.validate(parsed);
        organizationStagingService.persistRecords(batch, validation.validRecords());
        organizationStagingService.persistErrors(batch, validation.errors());
        HrSyncResult syncResult = organizationSynchronizationService.synchronize(batch, validation.validRecords());
        return new BatchProcessingResult(parsed.size(), syncResult.insertedRecords(), syncResult.updatedRecords(),
                validation.errors().size());
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

    private record BatchProcessingResult(int totalRecords, int insertedRecords, int updatedRecords, int failedRecords) {
    }
}
