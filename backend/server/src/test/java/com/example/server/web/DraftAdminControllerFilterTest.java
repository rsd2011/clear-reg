package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.draft.domain.ApprovalGroup;
import com.example.draft.domain.ApprovalLineTemplate;
import com.example.draft.domain.TemplateScope;
import com.example.draft.domain.repository.ApprovalGroupRepository;
import com.example.draft.domain.repository.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class DraftAdminControllerFilterTest {

    ApprovalLineTemplateRepository lineRepo = Mockito.mock(ApprovalLineTemplateRepository.class);
    DraftFormTemplateRepository formRepo = Mockito.mock(DraftFormTemplateRepository.class);
    ApprovalGroupRepository groupRepo = Mockito.mock(ApprovalGroupRepository.class);
    DraftAdminController controller = new DraftAdminController(lineRepo, formRepo, groupRepo);

    @Test
    @DisplayName("businessType 필터가 지정되면 해당 타입만 반환한다")
    void filterByBusinessType() {
        ApprovalLineTemplate a = ApprovalLineTemplate.create("name", "HR", "ORG", OffsetDateTime.now());
        ApprovalLineTemplate b = ApprovalLineTemplate.create("name2", "FIN", "ORG", OffsetDateTime.now());
        Mockito.when(lineRepo.findAll()).thenReturn(List.of(a, b));

        var filtered = controller.listApprovalTemplates("HR");

        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).businessType()).isEqualTo("HR");
    }

    @Test
    @DisplayName("organizationCode 필터가 지정되면 해당 조직의 그룹만 반환한다")
    void filterByOrganization() {
        ApprovalGroup g1 = ApprovalGroup.create("G1", "n1", "d", "ORG1", null, OffsetDateTime.now());
        ApprovalGroup g2 = ApprovalGroup.create("G2", "n2", "d", "ORG2", null, OffsetDateTime.now());
        Mockito.when(groupRepo.findAll()).thenReturn(List.of(g1, g2));

        var filtered = controller.listGroups("ORG1");

        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).organizationCode()).isEqualTo("ORG1");
    }
}
