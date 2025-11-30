package com.example.draft.application;
import com.example.admin.draft.service.TemplateAdminService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.context.AuthContext;
import com.example.common.orggroup.WorkType;
import com.example.common.security.RowScope;
import com.example.admin.draft.dto.DraftFormTemplateRequest;
import com.example.admin.draft.dto.DraftFormTemplateResponse;
import com.example.admin.draft.domain.DraftFormTemplateRoot;
import com.example.admin.approval.service.ApprovalTemplateRootService;
import com.example.admin.draft.repository.DraftFormTemplateRepository;
import com.example.admin.draft.repository.DraftFormTemplateRootRepository;

class TemplateAdminServiceUpdateFormTemplateTest {

    @Test
    @DisplayName("기안 양식 템플릿을 업데이트하면 새 버전이 생성된다")
    void updateFormTemplate() {
        DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
        DraftFormTemplateRootRepository rootRepo = mock(DraftFormTemplateRootRepository.class);
        TemplateAdminService service = new TemplateAdminService(
                mock(ApprovalTemplateRootService.class),
                formRepo,
                rootRepo);

        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000010");
        given(rootRepo.findById(id)).willReturn(Optional.of(root));
        given(formRepo.findMaxVersionByRoot(root)).willReturn(1);
        given(formRepo.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        DraftFormTemplateRequest req = new DraftFormTemplateRequest("form2", WorkType.GENERAL, "{\"f\":1}", true, null);
        AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, RowScope.ORG);

        DraftFormTemplateResponse res = service.updateDraftFormTemplate(id, req, ctx, false);

        assertThat(res.name()).isEqualTo("form2");
        assertThat(res.version()).isEqualTo(2);
    }
}
