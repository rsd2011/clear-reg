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
import org.springframework.data.domain.Sort;

import com.example.dw.application.DwOrganizationTreeService.OrganizationTreeSnapshot;
import com.example.dw.domain.HrOrganizationEntity;
import com.example.dw.infrastructure.persistence.HrOrganizationRepository;

@ExtendWith(MockitoExtension.class)
class DwOrganizationTreeServiceTest {

    @Mock
    private HrOrganizationRepository organizationRepository;

    private DwOrganizationTreeService service;

    @BeforeEach
    void setUp() {
        service = new DwOrganizationTreeService(organizationRepository);
    }

    @Test
    void givenOrganizations_whenSnapshot_thenSortedAndNavigable() {
        HrOrganizationEntity root = organization("ROOT", null);
        HrOrganizationEntity childB = organization("CHILD-B", "ROOT");
        HrOrganizationEntity childA = organization("CHILD-A", "ROOT");
        given(organizationRepository.findAll(Sort.by("organizationCode")))
                .willReturn(List.of(childB, root, childA));

        OrganizationTreeSnapshot snapshot = service.snapshot();

        assertThat(snapshot.flatten())
                .extracting(DwOrganizationNode::organizationCode)
                .containsExactly("CHILD-A", "CHILD-B", "ROOT");
        DwOrganizationNode rootNode = snapshot.node("ROOT").orElseThrow();
        assertThat(rootNode.name()).isEqualTo("Org-ROOT");
        assertThat(snapshot.descendantsIncluding("ROOT"))
                .extracting(DwOrganizationNode::organizationCode)
                .containsExactly("ROOT", "CHILD-A", "CHILD-B");
        assertThat(snapshot.ancestors("CHILD-A"))
                .extracting(DwOrganizationNode::organizationCode)
                .containsExactly("ROOT");
        verify(organizationRepository).findAll(Sort.by("organizationCode"));
    }

    @Test
    void givenMissingOrganization_whenNavigating_thenEmptyListsReturned() {
        HrOrganizationEntity root = organization("ROOT", null);
        given(organizationRepository.findAll(Sort.by("organizationCode")))
                .willReturn(List.of(root));

        OrganizationTreeSnapshot snapshot = service.snapshot();

        assertThat(snapshot.descendantsIncluding("UNKNOWN")).isEmpty();
        assertThat(snapshot.ancestors("UNKNOWN")).isEmpty();
        assertThat(snapshot.node("UNKNOWN")).isEmpty();
    }

    @Test
    void whenEvict_thenNoRepoInteraction() {
        service.evict();
    }

    private HrOrganizationEntity organization(String code, String parentCode) {
        HrOrganizationEntity entity = new HrOrganizationEntity();
        entity.setOrganizationCode(code);
        entity.setVersion(1);
        entity.setName("Org-" + code);
        entity.setParentOrganizationCode(parentCode);
        entity.setStatus("ACTIVE");
        entity.setEffectiveStart(LocalDate.of(2020, 1, 1));
        entity.setEffectiveEnd(null);
        entity.setSourceBatchId(UUID.randomUUID());
        entity.setSyncedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return entity;
    }
}
