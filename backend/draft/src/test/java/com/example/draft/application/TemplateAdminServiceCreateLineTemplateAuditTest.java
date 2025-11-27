package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.admin.approval.dto.ApprovalLineTemplateRequest;
import com.example.admin.approval.dto.ApprovalTemplateStepRequest;
import com.example.admin.approval.dto.ApprovalLineTemplateResponse;
import com.example.admin.approval.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class TemplateAdminServiceCreateLineTemplateAuditTest {

    @Test
    @DisplayName("audit=true일 때 템플릿이 정상적으로 저장된다")
    void createLineTemplateGlobalWhenAudit() {
        ApprovalLineTemplateRepository lineRepo = mock(ApprovalLineTemplateRepository.class);
        com.example.admin.approval.ApprovalGroupRepository groupRepo = mock(com.example.admin.approval.ApprovalGroupRepository.class);
        TemplateAdminService service = new TemplateAdminService(
                lineRepo,
                groupRepo,
                mock(DraftFormTemplateRepository.class), mock(com.example.draft.domain.repository.DraftTemplatePresetRepository.class), new com.fasterxml.jackson.databind.ObjectMapper());

        ApprovalLineTemplateRequest req = new ApprovalLineTemplateRequest(
                "line",
                0,
                null,
                true,
                List.of(new ApprovalTemplateStepRequest(1, "GRP")));

        AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, RowScope.ORG);

        // ApprovalGroup mock 설정
        OffsetDateTime now = OffsetDateTime.now();
        com.example.admin.approval.ApprovalGroup grp = com.example.admin.approval.ApprovalGroup.create("GRP", "그룹", "설명", 1, now);
        org.mockito.BDDMockito.given(groupRepo.findByGroupCode("GRP")).willReturn(java.util.Optional.of(grp));

        // save가 null을 반환하지 않도록 입력 그대로 반환
        org.mockito.BDDMockito.given(lineRepo.save(org.mockito.ArgumentMatchers.any())).willAnswer(invocation -> invocation.getArgument(0));

        ApprovalLineTemplateResponse res = service.createApprovalLineTemplate(req, ctx, true);

        assertThat(res.name()).isEqualTo("line");
        assertThat(res.steps()).hasSize(1);
    }
}
