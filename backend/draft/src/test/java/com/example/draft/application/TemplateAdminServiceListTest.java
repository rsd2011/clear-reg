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
    @DisplayName("listApprovalGroups는 모든 그룹을 반환한다")
    void listReturnsAllGroups() {
        ApprovalGroupRepository groupRepo = mock(ApprovalGroupRepository.class);
        ApprovalLineTemplateRepository lineRepo = mock(ApprovalLineTemplateRepository.class);
        DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
        TemplateAdminService service = new TemplateAdminService(groupRepo, lineRepo, formRepo, mock(com.example.draft.domain.repository.DraftTemplatePresetRepository.class), new com.fasterxml.jackson.databind.ObjectMapper());

        ApprovalGroup group1 = ApprovalGroup.create("G1", "name1", null, 0, OffsetDateTime.now());
        ApprovalGroup group2 = ApprovalGroup.create("G2", "name2", null, 10, OffsetDateTime.now());
        given(groupRepo.findAll()).willReturn(List.of(group1, group2));

        AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, RowScope.ORG);

        // audit=false든 true든 모든 그룹을 반환
        List<ApprovalGroupResponse> result = service.listApprovalGroups(null, ctx, false);
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ApprovalGroupResponse::groupCode).containsExactlyInAnyOrder("G1", "G2");
    }
}
