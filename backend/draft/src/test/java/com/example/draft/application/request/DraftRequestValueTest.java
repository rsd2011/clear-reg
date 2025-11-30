package com.example.draft.application.request;
import com.example.admin.draft.dto.DraftFormTemplateRequest;
import com.example.admin.draft.dto.DraftFormTemplateResponse;

import com.example.draft.application.dto.*;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.approval.dto.ApprovalTemplateRootRequest;
import com.example.admin.approval.dto.ApprovalTemplateStepRequest;
import com.example.common.orggroup.WorkType;

@DisplayName("Draft 요청 DTO 값 보존")
class DraftRequestValueTest {

    @Test
    @DisplayName("ApprovalTemplateStepRequest는 필드를 그대로 보존한다")
    void approvalStepRequestPreserves() {
        ApprovalTemplateStepRequest step = new ApprovalTemplateStepRequest(1, "GRP");
        assertThat(step.stepOrder()).isEqualTo(1);
        assertThat(step.approvalGroupCode()).isEqualTo("GRP");
    }

    @Test
    @DisplayName("ApprovalTemplateRootRequest는 리스트와 비즈니스 타입을 보존한다")
    void approvalLineTemplateRequestPreserves() {
        ApprovalTemplateStepRequest step = new ApprovalTemplateStepRequest(1, "GRP");
        ApprovalTemplateRootRequest request = new ApprovalTemplateRootRequest("name", 0, null, true, List.of(step));
        assertThat(request.name()).isEqualTo("name");
        assertThat(request.steps()).hasSize(1);
    }

    @Test
    @DisplayName("DraftFormTemplateRequest는 활성 여부와 스키마를 보존한다")
    void draftFormTemplateRequestPreserves() {
        DraftFormTemplateRequest req = new DraftFormTemplateRequest(
                "form", WorkType.HR_UPDATE, "{}", true, null);
        assertThat(req.name()).isEqualTo("form");
        assertThat(req.workType()).isEqualTo(WorkType.HR_UPDATE);
        assertThat(req.schemaJson()).isEqualTo("{}");
        assertThat(req.active()).isTrue();
    }
}
