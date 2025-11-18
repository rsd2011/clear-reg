package com.example.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.example.hr.domain.HrOrganizationEntity;
import com.example.hr.application.HrOrganizationQueryService;

@ExtendWith(MockitoExtension.class)
class HrOrganizationControllerTest {

    @Mock
    private HrOrganizationQueryService queryService;

    @InjectMocks
    private HrOrganizationController controller;

    @Test
    void givenOrganizations_whenListing_thenReturnPage() {
        HrOrganizationEntity entity = new HrOrganizationEntity();
        entity.setOrganizationCode("ORG001");
        entity.setVersion(1);
        entity.setName("Headquarters");
        entity.setParentOrganizationCode(null);
        entity.setStatus("ACTIVE");
        entity.setEffectiveStart(LocalDate.of(2020, 1, 1));
        entity.setSyncedAt(OffsetDateTime.now(ZoneOffset.UTC));

        PageRequest pageable = PageRequest.of(0, 20);
        given(queryService.getOrganizations(pageable)).willReturn(new PageImpl<>(List.of(entity), pageable, 1));

        Page<HrOrganizationResponse> response = controller.organizations(0, 20);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).organizationCode()).isEqualTo("ORG001");
        verify(queryService).getOrganizations(pageable);
    }
}
