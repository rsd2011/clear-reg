package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.approval.ApprovalGroup;
import com.example.admin.approval.ApprovalLineTemplate;
import com.example.admin.approval.ApprovalGroupRepository;
import com.example.admin.approval.ApprovalLineTemplateRepository;
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
    @DisplayName("활성화된 템플릿만 반환한다")
    void filtersTemplates() {
        ApprovalLineTemplate tpl1 = ApprovalLineTemplate.create("template1", 0, null, OffsetDateTime.now());
        ApprovalLineTemplate tpl2 = ApprovalLineTemplate.create("template2", 1, null, OffsetDateTime.now());
        org.mockito.BDDMockito.given(lineRepo.findAll()).willReturn(List.of(tpl1, tpl2));

        var templates = controller.listApprovalTemplates();

        assertThat(templates).hasSize(2);
    }

    @Test
    @DisplayName("activeOnly 필터가 적용된다")
    void filtersActiveGroups() {
        ApprovalGroup grp1 = ApprovalGroup.create("G1", "g1", "d", 0, OffsetDateTime.now());
        ApprovalGroup grp2 = ApprovalGroup.create("G2", "g2", "d", 1, OffsetDateTime.now());
        grp2.deactivate(OffsetDateTime.now());
        org.mockito.BDDMockito.given(groupRepo.findAll()).willReturn(List.of(grp1, grp2));

        var groups = controller.listGroups(true);

        assertThat(groups).hasSize(1);
        assertThat(groups.getFirst().groupCode()).isEqualTo("G1");
    }

    @Test
    @DisplayName("activeOnly=false이면 모든 그룹을 반환한다")
    void allowsAllWhenActiveOnlyFalse() {
        ApprovalGroup grp1 = ApprovalGroup.create("G1", "g1", "d", 0, OffsetDateTime.now());
        ApprovalGroup grp2 = ApprovalGroup.create("G2", "g2", "d", 1, OffsetDateTime.now());
        grp2.deactivate(OffsetDateTime.now());
        org.mockito.BDDMockito.given(groupRepo.findAll()).willReturn(List.of(grp1, grp2));

        var groups = controller.listGroups(false);

        assertThat(groups).hasSize(2);
    }
}
