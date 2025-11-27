package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.admin.approval.dto.ApprovalLineTemplateRequest;
import com.example.admin.approval.dto.ApprovalTemplateStepRequest;
import com.example.admin.approval.dto.ApprovalLineTemplateResponse;
import com.example.admin.approval.ApprovalLineTemplate;
import com.example.admin.approval.ApprovalTemplateStep;
import com.example.admin.approval.ApprovalGroupRepository;
import com.example.admin.approval.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class TemplateAdminServiceUpdateLineTemplateTest {

    @Test
    @DisplayName("라인 템플릿 업데이트 시 이름/active/steps가 교체된다")
    void updateLineTemplateReplacesSteps() {
        ApprovalLineTemplateRepository lineRepo = mock(ApprovalLineTemplateRepository.class);
        TemplateAdminService service = new TemplateAdminService(
                mock(ApprovalGroupRepository.class),
                lineRepo,
                mock(DraftFormTemplateRepository.class), mock(com.example.draft.domain.repository.DraftTemplatePresetRepository.class), new com.fasterxml.jackson.databind.ObjectMapper());

        OffsetDateTime now = OffsetDateTime.now();
        ApprovalLineTemplate template = ApprovalLineTemplate.create("old", "HR", "ORG1", now);
        template.replaceSteps(List.of(new ApprovalTemplateStep(template, 1, "GRP1", "desc1")));

        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000031");
        given(lineRepo.findById(id)).willReturn(Optional.of(template));
        given(lineRepo.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        ApprovalLineTemplateRequest req = new ApprovalLineTemplateRequest(
                "newName",
                "HR",
                "ORG1",
                true,
                List.of(
                        new ApprovalTemplateStepRequest(1, "GRP2", "d2"),
                        new ApprovalTemplateStepRequest(2, "GRP3", "d3")
                )
        );
        AuthContext ctx = new AuthContext("u", "ORG1", null, null, null, RowScope.ORG, null);

        ApprovalLineTemplateResponse res = service.updateApprovalLineTemplate(id, req, ctx, false);

        assertThat(res.name()).isEqualTo("newName");
        assertThat(res.steps()).hasSize(2);
        assertThat(res.active()).isTrue();
    }
}
