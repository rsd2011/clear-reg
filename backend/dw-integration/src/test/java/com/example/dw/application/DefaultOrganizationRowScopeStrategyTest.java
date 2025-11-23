package com.example.dw.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.example.dw.domain.HrOrganizationEntity;
import com.example.dw.infrastructure.persistence.HrOrganizationRepository;

class DefaultOrganizationRowScopeStrategyTest {

    HrOrganizationRepository repo = Mockito.mock(HrOrganizationRepository.class);
    DefaultOrganizationRowScopeStrategy strategy = new DefaultOrganizationRowScopeStrategy(repo);

    @Test
    @DisplayName("Given 조직코드 When apply 호출 Then 레포지토리 결과를 DwOrganizationNode로 변환한다")
    void applyReturnsNodes() {
        HrOrganizationEntity entity = new HrOrganizationEntity();
        entity.setOrganizationCode("ORG1");
        Page<HrOrganizationEntity> page = new PageImpl<>(java.util.List.of(entity));
        when(repo.findByOrganizationCode(Mockito.eq("ORG1"), Mockito.any())).thenReturn(page);

        Page<DwOrganizationNode> result = strategy.apply(PageRequest.of(0, 10), "ORG1");
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).organizationCode()).isEqualTo("ORG1");
    }
}
