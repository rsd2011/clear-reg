package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.draft.application.request.ApprovalLineTemplateRequest;
import com.example.draft.application.request.ApprovalTemplateStepRequest;
import com.example.draft.application.request.DraftFormTemplateRequest;
import com.example.draft.domain.DraftFormTemplate;
import com.example.draft.domain.exception.DraftAccessDeniedException;
import com.example.draft.domain.repository.ApprovalGroupRepository;
import com.example.draft.domain.repository.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

@ExtendWith(MockitoExtension.class)
class TemplateAdminServiceTest {

    private static final AuthContext AUDIT = new AuthContext("admin", "ORG-A", "PG", FeatureCode.DRAFT, ActionCode.DRAFT_AUDIT, RowScope.ALL, null);
    private static final AuthContext USER = new AuthContext("user", "ORG-A", "PG", FeatureCode.DRAFT, ActionCode.DRAFT_AUDIT, RowScope.SELF, null);

    @Mock
    private ApprovalGroupRepository approvalGroupRepository;
    @Mock
    private ApprovalLineTemplateRepository approvalLineTemplateRepository;
    @Mock
    private DraftFormTemplateRepository draftFormTemplateRepository;

    @InjectMocks
    private TemplateAdminService service;

    @BeforeEach
    void setup() {
        when(approvalLineTemplateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(draftFormTemplateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsGlobalApprovalLineTemplate_andOrdersSteps() {
        ApprovalLineTemplateRequest req = new ApprovalLineTemplateRequest(
                "휴가결재선", "LEAVE", null, true,
                List.of(
                        new ApprovalTemplateStepRequest(2, "DEPT_HEAD", "부서장"),
                        new ApprovalTemplateStepRequest(1, "TEAM_LEAD", "팀장")
                )
        );

        var response = service.createApprovalLineTemplate(req, AUDIT, true);

        assertThat(response.scope().name()).isEqualTo("GLOBAL");
        assertThat(response.steps()).extracting(s -> s.stepOrder()).containsExactly(1, 2);
        assertThat(response.steps()).extracting(s -> s.approvalGroupCode()).containsExactly("TEAM_LEAD", "DEPT_HEAD");
    }

    @Test
    void updateDraftFormTemplate_incrementsVersion_andUpdatesFields() {
        DraftFormTemplate template = DraftFormTemplate.create("기안양식", "LEAVE", "ORG-A", "{ }", OffsetDateTime.now());
        UUID id = template.getId();
        when(draftFormTemplateRepository.findById(id)).thenReturn(Optional.of(template));

        DraftFormTemplateRequest req = new DraftFormTemplateRequest("연차양식 V2", "LEAVE", "ORG-A", "{ \"type\":\"object\" }", false);

        var response = service.updateDraftFormTemplate(id, req, AUDIT, true);

        assertThat(response.version()).isEqualTo(2);
        assertThat(response.name()).isEqualTo("연차양식 V2");
        assertThat(response.schemaJson()).contains("type");
        assertThat(response.active()).isFalse();
    }

    @Test
    void deniesOrganizationMismatchWhenNotAudit() {
        ApprovalLineTemplateRequest req = new ApprovalLineTemplateRequest(
                "타조직결재선", "LEAVE", "ORG-B", true,
                List.of(new ApprovalTemplateStepRequest(1, "TEAM_LEAD", null))
        );

        assertThatThrownBy(() -> service.createApprovalLineTemplate(req, USER, false))
                .isInstanceOf(DraftAccessDeniedException.class);
    }
}
