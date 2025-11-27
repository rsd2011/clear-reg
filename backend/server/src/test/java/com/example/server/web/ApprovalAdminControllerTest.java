package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.approval.ApprovalGroup;
import com.example.admin.approval.ApprovalLineTemplate;
import com.example.admin.approval.TemplateScope;
import com.example.admin.approval.ApprovalGroupRepository;
import com.example.admin.approval.ApprovalLineTemplateRepository;
import com.example.common.security.RowScope;
import com.example.common.security.RowScopeContext;
import com.example.common.security.RowScopeContextHolder;

class ApprovalAdminControllerTest {

    ApprovalLineTemplateRepository lineRepo = org.mockito.Mockito.mock(ApprovalLineTemplateRepository.class);
    ApprovalGroupRepository groupRepo = org.mockito.Mockito.mock(ApprovalGroupRepository.class);
    ApprovalAdminController controller = new ApprovalAdminController(lineRepo, groupRepo);

    @AfterEach
    void tearDown() {
        RowScopeContextHolder.clear();
    }

    @Test
    @DisplayName("businessType/organization 필터가 적용된다")
    void filtersTemplatesAndGroups() {
        ApprovalLineTemplate orgTpl = ApprovalLineTemplate.create("org", "HR", "ORG1", OffsetDateTime.now());
        ApprovalLineTemplate other = ApprovalLineTemplate.create("other", "IT", "ORG2", OffsetDateTime.now());
        org.mockito.BDDMockito.given(lineRepo.findAll()).willReturn(List.of(orgTpl, other));

        ApprovalGroup grp1 = ApprovalGroup.create("G1", "g1", "d", "ORG1", null, OffsetDateTime.now());
        ApprovalGroup grp2 = ApprovalGroup.create("G2", "g2", "d", "ORG2", null, OffsetDateTime.now());
        org.mockito.BDDMockito.given(groupRepo.findAll()).willReturn(List.of(grp1, grp2));

        RowScopeContextHolder.set(new RowScopeContext("ORG1", List.of("ORG1")));

        var templates = controller.listApprovalTemplates("HR");
        var groups = controller.listGroups("ORG1");

        assertThat(templates).hasSize(1);
        assertThat(templates.getFirst().organizationCode()).isEqualTo("ORG1");
        assertThat(groups).extracting(ApprovalAdminController.ApprovalGroupSummary::organizationCode)
                .containsExactly("ORG1");
    }

    @Test
    @DisplayName("RowScope.ALL이면 모든 조직을 반환한다")
    void allowsAllWhenAllScope() {
        ApprovalGroup grp1 = ApprovalGroup.create("G1", "g1", "d", "ORG1", null, OffsetDateTime.now());
        org.mockito.BDDMockito.given(groupRepo.findAll()).willReturn(List.of(grp1));
        RowScopeContextHolder.set(new RowScopeContext(null, List.of()));

        var groups = controller.listGroups(null);

        assertThat(groups).hasSize(1);
        assertThat(groups.getFirst().organizationCode()).isEqualTo("ORG1");
    }
}
