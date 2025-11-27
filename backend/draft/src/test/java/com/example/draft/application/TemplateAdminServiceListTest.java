package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.admin.approval.dto.ApprovalGroupResponse;
import com.example.admin.approval.ApprovalGroup;
import com.example.admin.approval.ApprovalGroupRepository;
import com.example.admin.approval.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class TemplateAdminServiceListTest {

    @Test
    @DisplayName("audit=false이면 AuthContext 조직으로 필터링하고 audit=true이면 필터를 그대로 사용한다")
    void listFiltersOrgWhenNotAudit() {
        ApprovalGroupRepository groupRepo = mock(ApprovalGroupRepository.class);
        ApprovalLineTemplateRepository lineRepo = mock(ApprovalLineTemplateRepository.class);
        DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
        TemplateAdminService service = new TemplateAdminService(groupRepo, lineRepo, formRepo, mock(com.example.draft.domain.repository.DraftTemplatePresetRepository.class), new com.fasterxml.jackson.databind.ObjectMapper());

        ApprovalGroup org1 = ApprovalGroup.create("G1", "name", null, "ORG1", null, OffsetDateTime.now());
        ApprovalGroup org2 = ApprovalGroup.create("G2", "name", null, "ORG2", null, OffsetDateTime.now());
        given(groupRepo.findAll()).willReturn(List.of(org1, org2));

        AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, RowScope.ORG);

        // audit=false => context.organizationCode() 사용
        List<ApprovalGroupResponse> filtered = service.listApprovalGroups(null, ctx, false);
        assertThat(filtered).extracting(ApprovalGroupResponse::organizationCode).containsOnly("ORG1");

        // audit=true => 전달된 organizationCode 그대로 사용
        List<ApprovalGroupResponse> filteredAudit = service.listApprovalGroups("ORG2", ctx, true);
        assertThat(filteredAudit).extracting(ApprovalGroupResponse::organizationCode).containsOnly("ORG2");
    }
}
