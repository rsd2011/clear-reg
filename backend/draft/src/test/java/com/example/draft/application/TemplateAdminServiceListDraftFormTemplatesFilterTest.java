package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.context.AuthContext;
import com.example.common.orggroup.WorkType;
import com.example.common.security.RowScope;
import com.example.common.version.ChangeAction;
import com.example.draft.application.dto.DraftFormTemplateResponse;
import com.example.admin.draft.domain.DraftFormTemplate;
import com.example.admin.draft.domain.DraftFormTemplateRoot;
import com.example.admin.approval.service.ApprovalTemplateRootService;
import com.example.draft.domain.repository.DraftFormTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRootRepository;

class TemplateAdminServiceListDraftFormTemplatesFilterTest {

    @Test
    @DisplayName("workType 지정 + activeOnly=true일 때 해당 업무유형의 활성 템플릿만 반환한다")
    void filtersFormTemplatesByWorkTypeAndActive() {
        DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
        TemplateAdminService service = new TemplateAdminService(
                mock(ApprovalTemplateRootService.class),
                formRepo,
                mock(DraftFormTemplateRootRepository.class));

        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);
        DraftFormTemplate activeTemplate = createPublishedTemplate(root, "f1", WorkType.GENERAL, true, now);
        DraftFormTemplate inactiveTemplate = createPublishedTemplate(root, "f2", WorkType.GENERAL, false, now);

        given(formRepo.findCurrentByWorkType(WorkType.GENERAL)).willReturn(List.of(activeTemplate, inactiveTemplate));

        AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, RowScope.ORG);

        List<DraftFormTemplateResponse> result = service.listDraftFormTemplates(WorkType.GENERAL, true, ctx, false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("f1");
    }

    private DraftFormTemplate createPublishedTemplate(DraftFormTemplateRoot root, String name, WorkType workType, boolean active, OffsetDateTime now) {
        DraftFormTemplate template = DraftFormTemplate.create(
                root, 1, name, workType, "{}", active,
                ChangeAction.CREATE, null, "user", "user", now);
        // create()는 이미 PUBLISHED 상태로 생성됨
        return template;
    }
}
