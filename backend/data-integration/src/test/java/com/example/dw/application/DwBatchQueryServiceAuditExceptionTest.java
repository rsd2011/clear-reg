package com.example.dw.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.example.audit.AuditPort;
import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.domain.repository.HrBatchRepository;

class DwBatchQueryServiceAuditExceptionTest {

    HrBatchRepository repo = mock(HrBatchRepository.class);
    AuditPort auditPort = mock(AuditPort.class, invocation -> { throw new RuntimeException("audit down"); });

    @Test
    @DisplayName("auditPort 예외가 발생해도 목록 조회는 성공한다")
    void auditFailureDoesNotBreakList() {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(repo.findAll(pageable)).thenReturn(new PageImpl<>(java.util.List.of()))
                .thenReturn(Page.empty());

        DwBatchQueryService service = new DwBatchQueryService(repo, auditPort);
        Page<HrImportBatchEntity> page = service.getBatches(pageable);

        assertThat(page).isNotNull();
    }

    @Test
    @DisplayName("auditPort 예외가 발생해도 최신 배치 조회는 Optional을 그대로 반환한다")
    void auditFailureDoesNotBreakLatest() {
        when(repo.findLatest()).thenReturn(Optional.empty());
        DwBatchQueryService service = new DwBatchQueryService(repo, auditPort);

        Optional<HrImportBatchEntity> latest = service.latestBatch();
        assertThat(latest).isEmpty();
    }
}
