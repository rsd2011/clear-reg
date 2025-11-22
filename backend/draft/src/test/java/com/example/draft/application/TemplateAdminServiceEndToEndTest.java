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

import com.example.auth.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.draft.application.request.ApprovalGroupRequest;
import com.example.draft.application.request.ApprovalLineTemplateRequest;
import com.example.draft.application.request.ApprovalTemplateStepRequest;
import com.example.draft.application.request.DraftFormTemplateRequest;
import com.example.draft.application.response.ApprovalGroupResponse;
import com.example.draft.application.response.ApprovalLineTemplateResponse;
import com.example.draft.application.response.DraftFormTemplateResponse;
import com.example.draft.domain.ApprovalGroup;
import com.example.draft.domain.ApprovalLineTemplate;
import com.example.draft.domain.DraftFormTemplate;
import com.example.draft.domain.repository.ApprovalGroupRepository;
import com.example.draft.domain.repository.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class TemplateAdminServiceEndToEndTest {

    ApprovalGroupRepository groupRepo = mock(ApprovalGroupRepository.class);
    ApprovalLineTemplateRepository lineRepo = mock(ApprovalLineTemplateRepository.class);
    DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
    TemplateAdminService service = new TemplateAdminService(groupRepo, lineRepo, formRepo);
    AuthContext ctx = new AuthContext("u", "ORG1", null, null, null, RowScope.ORG, null);

    @Test
    @DisplayName("그룹/라인/폼 템플릿 생성 후 업데이트까지 일괄 플로우가 성공한다")
    void createAndUpdateEndToEnd() {
        OffsetDateTime now = OffsetDateTime.now();

        // create group
        given(groupRepo.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        ApprovalGroupRequest groupReq = new ApprovalGroupRequest("G1", "g-name", "desc", "ORG1", null);
        ApprovalGroupResponse groupRes = service.createApprovalGroup(groupReq, ctx, false);

        // update group backing repo
        ApprovalGroup groupEntity = ApprovalGroup.create("G1", "g-name", "desc", "ORG1", null, now);
        UUID groupId = UUID.fromString("00000000-0000-0000-0000-00000000e2e2");
        given(groupRepo.findById(groupId)).willReturn(Optional.of(groupEntity));
        ApprovalGroupResponse updatedGroup = service.updateApprovalGroup(groupId, new ApprovalGroupRequest("G1", "g-new", "d2", "ORG1", "expr"), ctx, false);

        // create line template
        given(lineRepo.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        ApprovalLineTemplateRequest lineReq = new ApprovalLineTemplateRequest("line", "HR", "ORG1", true,
                List.of(new ApprovalTemplateStepRequest(1, "G1", "desc")));
        ApprovalLineTemplateResponse lineRes = service.createApprovalLineTemplate(lineReq, ctx, false);

        // update line template
        ApprovalLineTemplate lineEntity = ApprovalLineTemplate.create("line", "HR", "ORG1", now);
        UUID lineId = UUID.fromString("00000000-0000-0000-0000-00000000f3f3");
        given(lineRepo.findById(lineId)).willReturn(Optional.of(lineEntity));
        given(lineRepo.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        ApprovalLineTemplateResponse updatedLine = service.updateApprovalLineTemplate(lineId, new ApprovalLineTemplateRequest("line2", "HR", "ORG1", true,
                List.of(new ApprovalTemplateStepRequest(1, "G1", "d"))), ctx, false);

        // create form template
        given(formRepo.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        DraftFormTemplateRequest formReq = new DraftFormTemplateRequest("form", "HR", "ORG1", "{\"f\":1}", true);
        DraftFormTemplateResponse formRes = service.createDraftFormTemplate(formReq, ctx, false);

        // update form template
        DraftFormTemplate formEntity = DraftFormTemplate.create("form", "HR", "ORG1", "{}", now);
        UUID formId = UUID.fromString("00000000-0000-0000-0000-00000000f4f4");
        given(formRepo.findById(formId)).willReturn(Optional.of(formEntity));
        given(formRepo.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        DraftFormTemplateResponse updatedForm = service.updateDraftFormTemplate(formId, new DraftFormTemplateRequest("form2", "HR", "ORG1", "{\"f\":2}", true), ctx, false);

        assertThat(groupRes.groupCode()).isEqualTo("G1");
        assertThat(updatedGroup.name()).isEqualTo("g-new");
        assertThat(lineRes.organizationCode()).isEqualTo("ORG1");
        assertThat(updatedLine.name()).isEqualTo("line2");
        assertThat(formRes.organizationCode()).isEqualTo("ORG1");
        assertThat(updatedForm.name()).isEqualTo("form2");
    }
}
