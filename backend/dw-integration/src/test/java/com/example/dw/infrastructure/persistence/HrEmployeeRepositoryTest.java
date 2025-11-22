package com.example.dw.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.example.dw.domain.HrEmployeeEntity;

@DataJpaTest
@Import(DwIntegrationJpaTestConfig.class)
class HrEmployeeRepositoryTest {

    @Autowired
    HrEmployeeRepository repository;

    @Test
    @DisplayName("employeeId로 최신 활성 레코드를 조회한다")
    void findActiveReturnsLatest() {
        HrEmployeeEntity older = new HrEmployeeEntity();
        older.setEmployeeId("E1");
        older.setVersion(1);
        older.setFullName("Old");
        older.setEffectiveStart(LocalDate.parse("2024-01-01"));
        older.setSourceBatchId(java.util.UUID.randomUUID());
        older.setSyncedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
        repository.save(older);

        HrEmployeeEntity newer = new HrEmployeeEntity();
        newer.setEmployeeId("E1");
        newer.setVersion(2);
        newer.setFullName("New");
        newer.setEffectiveStart(LocalDate.parse("2024-01-02"));
        newer.setSourceBatchId(java.util.UUID.randomUUID());
        repository.save(newer);

        Optional<HrEmployeeEntity> active = repository.findActive("E1");

        assertThat(active).isPresent();
        assertThat(active.get().getVersion()).isEqualTo(2);
        assertThat(active.get().getFullName()).isEqualTo("New");
    }
}
