package com.example.dw.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.domain.repository.HrBatchRepository;
import com.example.testing.bdd.Scenario;

@ExtendWith(MockitoExtension.class)
class DwBatchQueryServiceTest {

    @Mock
    private HrBatchRepository repository;
    @Mock
    private AuditPort auditPort;

    private DwBatchQueryService service;

    @BeforeEach
    void setUp() {
        service = new DwBatchQueryService(repository, auditPort);
    }

    @Test
    void givenBatches_whenListing_thenFetchFromRepository() {
        PageRequest pageable = PageRequest.of(0, 20);
        Page<HrImportBatchEntity> batches = new PageImpl<>(java.util.List.of(new HrImportBatchEntity()));
        given(repository.findAll(pageable)).willReturn(batches);

        Scenario.given("배치 리스트 조회", () -> service)
                .when("페이지 요청", svc -> svc.getBatches(pageable))
                .then("리포지토리에서 Fetch", page -> {
                    verify(repository).findAll(pageable);
                    assertThat(page).isEqualTo(batches);
                    verify(auditPort).record(Mockito.any(), Mockito.eq(AuditMode.ASYNC_FALLBACK));
                });
    }

    @Test
    void givenLatestBatch_whenQuery_thenReturnOptional() {
        HrImportBatchEntity latest = new HrImportBatchEntity();
        given(repository.findLatest()).willReturn(Optional.of(latest));

        Scenario.given("최신 배치 조회", () -> service)
                .when("latestBatch 호출", DwBatchQueryService::latestBatch)
                .then("Optional 로 감싼 배치 반환", optional -> {
                    verify(repository).findLatest();
                    assertThat(optional).contains(latest);
                    verify(auditPort).record(Mockito.any(), Mockito.eq(AuditMode.ASYNC_FALLBACK));
                });
    }
}
