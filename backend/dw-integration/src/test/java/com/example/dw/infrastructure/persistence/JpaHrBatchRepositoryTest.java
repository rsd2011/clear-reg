package com.example.dw.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import com.example.dw.domain.HrBatchStatus;
import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.dto.DataFeedType;

@DataJpaTest
@Import({DwIntegrationJpaTestConfig.class, JpaHrBatchRepository.class})
class JpaHrBatchRepositoryTest {

    @Autowired
    JpaHrBatchRepository repository;

    @Autowired
    SpringDataHrImportBatchRepository delegate;

    @Test
    @DisplayName("최신 배치를 조회하고 페이지 정렬로 반환한다")
    void findLatestAndPaged() {
        HrImportBatchEntity first = buildBatch("file1.csv", 1);
        first.setStatus(HrBatchStatus.RECEIVED);
        delegate.save(first);

        HrImportBatchEntity second = buildBatch("file2.csv", 2);
        second.setStatus(HrBatchStatus.COMPLETED);
        delegate.save(second);

        assertThat(repository.findLatest()).isPresent()
                .get().extracting(HrImportBatchEntity::getFileName).isEqualTo("file2.csv");

        var page = repository.findAll(PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getFileName()).isEqualTo("file2.csv");
    }

    private HrImportBatchEntity buildBatch(String fileName, int seq) {
        HrImportBatchEntity entity = new HrImportBatchEntity();
        entity.setFileName(fileName);
        entity.setFeedType(DataFeedType.EMPLOYEE);
        entity.setSourceName("SRC");
        entity.setBusinessDate(LocalDate.parse("2024-02-01"));
        entity.setSequenceNumber(seq);
        entity.setChecksum(UUID.randomUUID().toString());
        entity.setSourcePath("/tmp/" + fileName);
        entity.setTotalRecords(10);
        entity.setInsertedRecords(5);
        entity.setUpdatedRecords(5);
        return entity;
    }
}
