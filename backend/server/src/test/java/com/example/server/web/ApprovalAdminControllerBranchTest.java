package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.approval.ApprovalGroup;
import com.example.admin.approval.ApprovalLineTemplate;
import com.example.admin.approval.ApprovalGroupRepository;
import com.example.admin.approval.ApprovalLineTemplateRepository;
import com.example.common.policy.DataPolicyContextHolder;
import com.example.common.policy.DataPolicyMatch;
import com.example.common.security.RowScope;
import com.example.common.security.RowScopeContext;
import com.example.common.security.RowScopeContextHolder;

class ApprovalAdminControllerBranchTest {

    ApprovalLineTemplateRepository lineRepo = org.mockito.Mockito.mock(ApprovalLineTemplateRepository.class);
    ApprovalGroupRepository groupRepo = org.mockito.Mockito.mock(ApprovalGroupRepository.class);
    ApprovalAdminController controller = new ApprovalAdminController(lineRepo, groupRepo);

    @AfterEach
    void cleanup() {
        DataPolicyContextHolder.clear();
        RowScopeContextHolder.clear();
    }

    @Test
    @DisplayName("RowScope.ALL이면 조직 필터 없이 템플릿을 반환한다")
    void listApprovalTemplates_allScope() {
        ApprovalLineTemplate t1 = ApprovalLineTemplate.create("name1", "HR", null, OffsetDateTime.now());
        ApprovalLineTemplate t2 = ApprovalLineTemplate.create("name2", "HR", "ORG1", OffsetDateTime.now());
        given(lineRepo.findAll()).willReturn(List.of(t1, t2));

        var result = controller.listApprovalTemplates("HR");

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("RowScope.ORG이면 허용 조직만 반환한다")
    void listApprovalTemplates_orgScope() {
        DataPolicyContextHolder.set(DataPolicyMatch.builder().rowScope(RowScope.ORG.name()).build());
        RowScopeContextHolder.set(new RowScopeContext("ORG1", List.of("ORG1")));
        ApprovalLineTemplate org1 = ApprovalLineTemplate.create("name1", "HR", "ORG1", OffsetDateTime.now());
        ApprovalLineTemplate org2 = ApprovalLineTemplate.create("name2", "HR", "ORG2", OffsetDateTime.now());
        given(lineRepo.findAll()).willReturn(List.of(org1, org2));

        var result = controller.listApprovalTemplates(null);

        assertThat(result).extracting(ApprovalAdminController.ApprovalLineTemplateSummary::organizationCode)
                .containsExactly("ORG1");
    }

    @Test
    @DisplayName("RowScope.OWN이면 organizationCode가 null인 그룹은 제외된다")
    void listApprovalGroups_ownScope() {
        DataPolicyContextHolder.set(DataPolicyMatch.builder().rowScope(RowScope.OWN.name()).build());
        RowScopeContextHolder.set(new RowScopeContext("ORG1", List.of()));
        ApprovalGroup g1 = ApprovalGroup.create("G1", "name", "desc", "ORG1", null, OffsetDateTime.now());
        ApprovalGroup gNull = ApprovalGroup.create("G2", "name", "desc", null, null, OffsetDateTime.now());
        given(groupRepo.findAll()).willReturn(List.of(g1, gNull));

        var result = controller.listGroups(null);

        assertThat(result).extracting(ApprovalAdminController.ApprovalGroupSummary::organizationCode)
                .containsExactly("ORG1");
    }
}

