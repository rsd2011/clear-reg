package com.example.dw.application.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.common.security.RowScope;
import com.example.dw.application.DwOrganizationNode;
import com.example.dw.application.DwOrganizationQueryService;

@ExtendWith(MockitoExtension.class)
@DisplayName("DwOrganizationPortAdapter 테스트")
class DwOrganizationPortAdapterTest {

    @Mock
    private DwOrganizationQueryService queryService;

    @InjectMocks
    private DwOrganizationPortAdapter adapter;

    @Test
    @DisplayName("Given 조직 데이터가 존재할 때 When 조회하면 Then 조직 레코드를 반환한다")
    void givenOrganizations_whenQuerying_thenReturnRecords() {
        UUID id = UUID.randomUUID();
        DwOrganizationNode node = new DwOrganizationNode(
                id, "ORG001", 1, "Headquarters", null, "ACTIVE",
                LocalDate.of(2020, 1, 1), null, UUID.randomUUID(),
                OffsetDateTime.now(ZoneOffset.UTC));
        Pageable pageable = PageRequest.of(0, 10);
        given(queryService.getOrganizations(pageable, RowScope.ALL, "ORG001"))
                .willReturn(new PageImpl<>(List.of(node)));

        Page<DwOrganizationPort.DwOrganizationRecord> result =
                adapter.getOrganizations(pageable, RowScope.ALL, "ORG001");

        assertThat(result.getContent()).hasSize(1);
        DwOrganizationPort.DwOrganizationRecord record = result.getContent().get(0);
        assertThat(record.id()).isEqualTo(id);
        assertThat(record.organizationCode()).isEqualTo("ORG001");
        assertThat(record.name()).isEqualTo("Headquarters");
    }

    @Test
    @DisplayName("Given 조직이 없을 때 When 조회하면 Then 빈 페이지를 반환한다")
    void givenNoOrganizations_whenQuerying_thenReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        given(queryService.getOrganizations(pageable, RowScope.OWN, "ORG002"))
                .willReturn(Page.empty());

        Page<DwOrganizationPort.DwOrganizationRecord> result =
                adapter.getOrganizations(pageable, RowScope.OWN, "ORG002");

        assertThat(result.getContent()).isEmpty();
    }
}
