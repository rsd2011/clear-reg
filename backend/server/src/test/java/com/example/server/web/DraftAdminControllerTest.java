package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.draft.domain.ApprovalGroup;
import com.example.draft.domain.ApprovalLineTemplate;
import com.example.draft.domain.DraftFormTemplate;
import com.example.draft.domain.TemplateScope;
import com.example.draft.domain.repository.ApprovalGroupRepository;
import com.example.draft.domain.repository.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class DraftAdminControllerTest {

    ApprovalLineTemplateRepository approvalRepo = Mockito.mock(ApprovalLineTemplateRepository.class);
    DraftFormTemplateRepository formRepo = Mockito.mock(DraftFormTemplateRepository.class);
    ApprovalGroupRepository groupRepo = Mockito.mock(ApprovalGroupRepository.class);
    DraftAdminController controller = new DraftAdminController(approvalRepo, formRepo, groupRepo);

    @Test
    @DisplayName("businessType 필터가 null이면 모든 승인 템플릿을 반환한다")
    void listApproval_noFilter_returnsAll() {
        ApprovalLineTemplate t1 = template("HR");
        ApprovalLineTemplate t2 = template("IT");
        given(approvalRepo.findAll()).willReturn(List.of(t1, t2));

        List<DraftAdminController.ApprovalLineTemplateSummary> result = controller.listApprovalTemplates(null);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("organizationCode 필터가 일치하는 그룹만 반환한다")
    void listGroups_filtersByOrg() {
        ApprovalGroup g1 = group("ORG1");
        ApprovalGroup g2 = group("ORG2");
        given(groupRepo.findAll()).willReturn(List.of(g1, g2));

        List<DraftAdminController.ApprovalGroupSummary> result = controller.listGroups("ORG1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).organizationCode()).isEqualTo("ORG1");
    }

    private ApprovalLineTemplate template(String businessType) {
        ApprovalLineTemplate t = ApprovalLineTemplate.create("name", businessType, "ORG", java.time.OffsetDateTime.now());
        return t;
    }

    private ApprovalGroup group(String org) {
        return ApprovalGroup.create("G-" + org, "n", "desc", org, null, java.time.OffsetDateTime.now());
    }
}
