package com.example.admin.draft.service;
import com.example.admin.draft.service.DraftFormTemplateService;

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
import com.example.admin.draft.dto.DraftFormTemplateResponse;
import com.example.admin.draft.domain.DraftFormTemplate;
import com.example.admin.draft.domain.DraftFormTemplateRoot;
import com.example.admin.draft.repository.DraftFormTemplateRepository;
import com.example.admin.draft.repository.DraftFormTemplateRootRepository;

class DraftFormTemplateServiceListDraftFormTemplatesActiveFalseTest {

    @Test
    @DisplayName("activeOnly=false이면 모든 활성/비활성 템플릿을 반환한다")
    void returnsAllTemplatesWhenActiveOnlyIsFalse() {
        DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
        DraftFormTemplateService service = new DraftFormTemplateService(
                formRepo, mock(DraftFormTemplateRootRepository.class));

        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);
        DraftFormTemplate active = createPublishedTemplate(root, "f1", WorkType.GENERAL, true, now);
        DraftFormTemplate inactive = createPublishedTemplate(root, "f2", WorkType.GENERAL, false, now);

        given(formRepo.findCurrentByWorkType(WorkType.GENERAL)).willReturn(List.of(active, inactive));

        AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, RowScope.ORG);

        List<DraftFormTemplateResponse> result = service.listDraftFormTemplates(WorkType.GENERAL, false, ctx, false);

        assertThat(result).extracting(DraftFormTemplateResponse::name)
                .containsExactlyInAnyOrder("f1", "f2");
    }

    private DraftFormTemplate createPublishedTemplate(DraftFormTemplateRoot root, String name, WorkType workType, boolean active, OffsetDateTime now) {
        DraftFormTemplate template = DraftFormTemplate.create(
                root, 1, name, workType, "{}", active,
                ChangeAction.CREATE, null, "user", "user", now);
        // create()는 이미 PUBLISHED 상태로 생성됨
        return template;
    }
}
