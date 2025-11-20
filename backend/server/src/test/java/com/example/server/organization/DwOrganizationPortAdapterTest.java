package com.example.server.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.example.common.security.RowScope;
import com.example.dw.application.DwOrganizationNode;
import com.example.dw.application.DwOrganizationQueryService;

@DisplayName("DwOrganizationPortAdapter 테스트")
class DwOrganizationPortAdapterTest {

    private final DwOrganizationQueryService queryService = Mockito.mock(DwOrganizationQueryService.class);
    private final DwOrganizationPortAdapter adapter = new DwOrganizationPortAdapter(queryService);

    @Test
    @DisplayName("서비스 결과를 포트 레코드로 변환한다")
    void getOrganizations() {
        Pageable pageable = Pageable.unpaged();
        DwOrganizationNode node = new DwOrganizationNode(java.util.UUID.randomUUID(), "ORG001", 1,
                "Headquarters", null, "ACTIVE",
                LocalDate.of(2020, 1, 1), null, java.util.UUID.randomUUID(),
                OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        given(queryService.getOrganizations(pageable, RowScope.ALL, "ORG001"))
                .willReturn(new PageImpl<>(List.of(node)));

        var page = adapter.getOrganizations(pageable, RowScope.ALL, "ORG001");

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).organizationCode()).isEqualTo("ORG001");
        then(queryService).should().getOrganizations(pageable, RowScope.ALL, "ORG001");
    }
}
