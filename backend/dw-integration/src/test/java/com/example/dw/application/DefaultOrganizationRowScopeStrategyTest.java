package com.example.dw.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.example.dw.domain.HrOrganizationEntity;
import com.example.dw.infrastructure.persistence.HrOrganizationRepository;

@ExtendWith(MockitoExtension.class)
class DefaultOrganizationRowScopeStrategyTest {

    @Mock
    private HrOrganizationRepository organizationRepository;

    private DefaultOrganizationRowScopeStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new DefaultOrganizationRowScopeStrategy(organizationRepository);
    }

    @Test
    void givenOrganization_whenApply_thenDelegatesToRepository() {
        PageRequest pageable = PageRequest.of(0, 5);
        HrOrganizationEntity entity = organization("ORG-123", "ROOT");
        Page<HrOrganizationEntity> page = new PageImpl<>(List.of(entity), pageable, 1);
        given(organizationRepository.findByOrganizationCode("ORG-123", pageable)).willReturn(page);

        Page<DwOrganizationNode> result = strategy.apply(pageable, "ORG-123");

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().organizationCode()).isEqualTo("ORG-123");
        verify(organizationRepository).findByOrganizationCode("ORG-123", pageable);
    }

    private HrOrganizationEntity organization(String code, String parentCode) {
        HrOrganizationEntity entity = new HrOrganizationEntity();
        entity.setOrganizationCode(code);
        entity.setVersion(2);
        entity.setName("Org-" + code);
        entity.setParentOrganizationCode(parentCode);
        entity.setStatus("ACTIVE");
        entity.setEffectiveStart(LocalDate.of(2020, 5, 1));
        entity.setEffectiveEnd(null);
        entity.setSourceBatchId(UUID.randomUUID());
        entity.setSyncedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return entity;
    }
}
