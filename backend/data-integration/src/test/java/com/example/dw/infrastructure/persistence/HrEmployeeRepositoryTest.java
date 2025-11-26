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
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        HrEmployeeEntity older = HrEmployeeEntity.snapshot(
                "E1", 1, "Old", null, null, null, null,
                LocalDate.parse("2024-01-01"), null, java.util.UUID.randomUUID(), now.minusDays(1));
        repository.save(older);

        HrEmployeeEntity newer = HrEmployeeEntity.snapshot(
                "E1", 2, "New", null, null, null, null,
                LocalDate.parse("2024-01-02"), null, java.util.UUID.randomUUID(), now);
        repository.save(newer);

        Optional<HrEmployeeEntity> active = repository.findActive("E1");

        assertThat(active).isPresent();
        assertThat(active.get().getVersion()).isEqualTo(2);
        assertThat(active.get().getFullName()).isEqualTo("New");
    }
}
