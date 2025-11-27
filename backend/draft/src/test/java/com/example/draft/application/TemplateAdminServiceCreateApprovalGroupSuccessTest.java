package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.admin.approval.dto.ApprovalGroupRequest;
import com.example.admin.approval.dto.ApprovalGroupResponse;
import com.example.admin.approval.ApprovalGroupRepository;
import com.example.admin.approval.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class TemplateAdminServiceCreateApprovalGroupSuccessTest {

    @Test
    @DisplayName("approvalGroup 생성 성공 시 저장된 엔티티를 반환한다")
    void createApprovalGroupSuccess() {
        ApprovalGroupRepository groupRepo = mock(ApprovalGroupRepository.class);
        TemplateAdminService service = new TemplateAdminService(
                groupRepo,
                mock(ApprovalLineTemplateRepository.class),
                mock(DraftFormTemplateRepository.class), mock(com.example.draft.domain.repository.DraftTemplatePresetRepository.class), new com.fasterxml.jackson.databind.ObjectMapper());

        given(groupRepo.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        ApprovalGroupRequest req = new ApprovalGroupRequest("G1", "name", "desc", 10);
        AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, RowScope.ORG);

        ApprovalGroupResponse res = service.createApprovalGroup(req, ctx, false);

        assertThat(res.groupCode()).isEqualTo("G1");
        assertThat(res.name()).isEqualTo("name");
        assertThat(res.priority()).isEqualTo(10);
    }
}
