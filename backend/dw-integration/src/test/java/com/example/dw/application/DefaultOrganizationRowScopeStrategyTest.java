package com.example.dw.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.example.dw.infrastructure.persistence.HrOrganizationRepository;
import com.example.dw.domain.HrOrganizationEntity;

@ExtendWith(MockitoExtension.class)
class DefaultOrganizationRowScopeStrategyTest {

    @Mock
    HrOrganizationRepository repository;

    @InjectMocks
    DefaultOrganizationRowScopeStrategy strategy;

    @Test
    @DisplayName("조직 코드와 페이지 정보를 사용해 리포지토리를 조회하고 노드로 매핑한다")
    void appliesRepositoryAndMapsNode() {
        var pageable = PageRequest.of(0, 5);
        HrOrganizationEntity entity = new HrOrganizationEntity();
        entity.setOrganizationCode("ORG");
        entity.setName("Org");
        entity.setVersion(1);
        when(repository.findByOrganizationCode("ORG", pageable))
                .thenReturn(new PageImpl<>(java.util.List.of(entity), pageable, 1));

        var page = strategy.apply(pageable, "ORG");

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().organizationCode()).isEqualTo("ORG");
        verify(repository).findByOrganizationCode("ORG", pageable);
    }
}
