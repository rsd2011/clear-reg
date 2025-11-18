package com.example.batch.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.batch.ingestion.feed.HrFeed;
import com.example.batch.ingestion.feed.HrFeedConnector;
import com.example.hr.domain.HrBatchStatus;
import com.example.hr.domain.HrImportBatchEntity;
import com.example.hr.domain.repository.HrBatchRepository;
import com.example.hr.dto.HrEmployeeRecord;
import com.example.hr.dto.HrFeedType;
import com.example.hr.dto.HrOrganizationRecord;
import com.example.hr.dto.HrOrganizationValidationError;
import com.example.hr.dto.HrOrganizationValidationResult;
import com.example.hr.dto.HrSyncResult;
import com.example.hr.dto.HrValidationResult;
import com.example.testing.bdd.Scenario;

@ExtendWith(MockitoExtension.class)
class HrIngestionServiceTest {

    @Mock
    private HrCsvRecordParser parser;
    @Mock
    private HrRecordValidator validator;
    @Mock
    private HrStagingService stagingService;
    @Mock
    private HrEmployeeSynchronizationService employeeSyncService;
    @Mock
    private HrOrganizationCsvParser organizationCsvParser;
    @Mock
    private HrOrganizationValidator organizationValidator;
    @Mock
    private HrOrganizationStagingService organizationStagingService;
    @Mock
    private HrOrganizationSynchronizationService organizationSyncService;
    @Mock
    private HrBatchRepository batchRepository;
    @Mock
    private HrFeedConnector primaryConnector;
    @Mock
    private HrFeedConnector secondaryConnector;

    private HrIngestionService service;

    @BeforeEach
    void setUp() {
        service = new HrIngestionService(List.of(primaryConnector, secondaryConnector), parser, validator,
                stagingService, employeeSyncService, organizationCsvParser, organizationValidator,
                organizationStagingService, organizationSyncService, batchRepository);
        given(batchRepository.save(any(HrImportBatchEntity.class))).willAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void givenEmployeeFeed_whenIngest_thenProcessPipelineAndCompleteBatch() {
        HrFeed feed = new HrFeed("employees.csv", HrFeedType.EMPLOYEE, LocalDate.now(), 1,
                "payload", "sftp", Map.of());
        given(primaryConnector.nextFeed()).willReturn(Optional.empty());
        given(secondaryConnector.nextFeed()).willReturn(Optional.of(feed));

        List<HrEmployeeRecord> records = List.of(new HrEmployeeRecord("E-1", "Kim", "kim@example.com", "ORG1",
                "FULL", "ACTIVE", LocalDate.now(), null, feed.payload(), 1));
        HrValidationResult validation = new HrValidationResult(records, List.of());
        given(parser.parse(feed.payload())).willReturn(records);
        given(validator.validate(records)).willReturn(validation);
        given(employeeSyncService.synchronize(any(HrImportBatchEntity.class), eq(records))).willReturn(new HrSyncResult(1, 0));

        Scenario.given("직원 피드 처리", service::ingestNextFile)
                .then("배치가 성공적으로 완료", maybeBatch -> {
                    assertThat(maybeBatch).isPresent();
                    HrImportBatchEntity batch = maybeBatch.get();
                    assertThat(batch.getStatus()).isEqualTo(HrBatchStatus.COMPLETED);
                    verify(secondaryConnector).onSuccess(feed);
                    verify(stagingService).persistRecords(batch, records);
                    verify(stagingService).persistErrors(batch, validation.errors());
                });
    }

    @Test
    void givenOrganizationFeedFailure_whenIngest_thenMarksBatchAsFailed() {
        HrFeed feed = new HrFeed("org.csv", HrFeedType.ORGANIZATION, LocalDate.now(), 7,
                null, "s3", Map.of());
        given(primaryConnector.nextFeed()).willReturn(Optional.of(feed));
        IllegalStateException parsingError = new IllegalStateException("parse error");
        given(organizationCsvParser.parse(null)).willThrow(parsingError);

        Scenario.given("조직 피드 처리 중 예외", service::ingestNextFile)
                .then("배치가 실패 상태", maybeBatch -> {
                    assertThat(maybeBatch).isPresent();
                    HrImportBatchEntity batch = maybeBatch.get();
                    assertThat(batch.getStatus()).isEqualTo(HrBatchStatus.FAILED);
                    assertThat(batch.getCompletedAt()).isAfter(batch.getReceivedAt());
                    verify(primaryConnector).onFailure(feed, parsingError);
                });
    }

    @Test
    void givenOrganizationFeed_whenSuccessful_thenSynchronizeWithValidator() {
        HrFeed feed = new HrFeed("org.csv", HrFeedType.ORGANIZATION, LocalDate.now(), 3,
                "org payload", "sftp", Map.of());
        given(primaryConnector.nextFeed()).willReturn(Optional.empty());
        given(secondaryConnector.nextFeed()).willReturn(Optional.of(feed));

        List<HrOrganizationRecord> records = List.of(new HrOrganizationRecord("ORG", "Org", null, "ACTIVE",
                LocalDate.now(), null, feed.payload(), 1));
        HrOrganizationValidationResult validation = new HrOrganizationValidationResult(records,
                List.of(new HrOrganizationValidationError(2, "CHILD", "ERR", "message", "raw")));

        given(organizationCsvParser.parse(feed.payload())).willReturn(records);
        given(organizationValidator.validate(records)).willReturn(validation);
        given(organizationSyncService.synchronize(any(HrImportBatchEntity.class), eq(records)))
                .willReturn(new HrSyncResult(2, 1));

        Scenario.given("조직 피드 성공 처리", service::ingestNextFile)
                .then("조직 스테이징과 동기화 호출", maybeBatch -> {
                    assertThat(maybeBatch).isPresent();
                    HrImportBatchEntity batch = maybeBatch.get();
                    assertThat(batch.getStatus()).isEqualTo(HrBatchStatus.COMPLETED);
                    verify(organizationStagingService).persistRecords(batch, records);
                    verify(organizationStagingService).persistErrors(batch, validation.errors());
                    verify(organizationSyncService).synchronize(batch, records);
                });
    }
}
