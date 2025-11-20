package com.example.batch.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

import com.example.batch.ingestion.feed.DataFeed;
import com.example.batch.ingestion.feed.DataFeedConnector;
import com.example.batch.ingestion.template.DwFeedIngestionTemplate;
import com.example.batch.ingestion.template.DwIngestionResult;
import com.example.dw.domain.HrBatchStatus;
import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.domain.repository.HrBatchRepository;
import com.example.dw.dto.DataFeedType;
import com.example.testing.bdd.Scenario;

@ExtendWith(MockitoExtension.class)
class DwIngestionServiceTest {

    @Mock
    private DataFeedConnector primaryConnector;
    @Mock
    private DataFeedConnector secondaryConnector;
    @Mock
    private HrBatchRepository batchRepository;
    @Mock
    private DwFeedIngestionTemplate employeeTemplate;
    @Mock
    private DwFeedIngestionTemplate organizationTemplate;

    private DwIngestionService service;

    @BeforeEach
    void setUp() {
        given(employeeTemplate.supportedType()).willReturn(DataFeedType.EMPLOYEE);
        given(organizationTemplate.supportedType()).willReturn(DataFeedType.ORGANIZATION);
        given(batchRepository.save(any(HrImportBatchEntity.class))).willAnswer(invocation -> invocation.getArgument(0));
        service = new DwIngestionService(List.of(primaryConnector, secondaryConnector),
                List.of(employeeTemplate, organizationTemplate), batchRepository);
    }

    @Test
    void givenEmployeeFeed_whenProcessed_thenUseTemplate() {
        DataFeed feed = new DataFeed("employees.csv", DataFeedType.EMPLOYEE, LocalDate.now(), 1,
                "payload", "sftp", Map.of());
        given(primaryConnector.nextFeed()).willReturn(Optional.empty());
        given(secondaryConnector.nextFeed()).willReturn(Optional.of(feed));
        given(employeeTemplate.ingest(any(), any())).willReturn(new DwIngestionResult(1, 1, 0, 0));

        Scenario.given("직원 피드", service::ingestNextFile)
                .then("배치 완료", maybeBatch -> {
                    assertThat(maybeBatch).isPresent();
                    assertThat(maybeBatch.get().getStatus()).isEqualTo(HrBatchStatus.COMPLETED);
                    verify(employeeTemplate).ingest(any(), any());
                    verify(secondaryConnector).onSuccess(feed);
                });
    }

    @Test
    void givenTemplateThrows_whenProcessing_thenMarkFailed() {
        DataFeed feed = new DataFeed("org.csv", DataFeedType.ORGANIZATION, LocalDate.now(), 1,
                "payload", "sftp", Map.of());
        given(primaryConnector.nextFeed()).willReturn(Optional.of(feed));
        RuntimeException error = new RuntimeException("boom");
        given(organizationTemplate.ingest(any(), any())).willThrow(error);

        Scenario.given("조직 피드 실패", service::ingestNextFile)
                .then("배치 실패", maybeBatch -> {
                    assertThat(maybeBatch).isPresent();
                    assertThat(maybeBatch.get().getStatus()).isEqualTo(HrBatchStatus.FAILED);
                    verify(primaryConnector).onFailure(feed, error);
                });
    }
}
