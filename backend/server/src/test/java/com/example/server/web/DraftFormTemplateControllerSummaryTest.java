package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.orggroup.WorkType;
import com.example.common.security.RowScope;
import com.example.common.version.ChangeAction;
import com.example.admin.draft.domain.DraftFormTemplate;
import com.example.admin.draft.domain.DraftFormTemplateRoot;
import com.example.admin.draft.service.DraftFormTemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class DraftFormTemplateControllerSummaryTest {

    DraftFormTemplateService service = org.mockito.Mockito.mock(DraftFormTemplateService.class);
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    DraftFormTemplateController controller = new DraftFormTemplateController(service, objectMapper);

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    private DraftFormTemplate createPublishedTemplate(String name, WorkType workType, OffsetDateTime now) {
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);
        DraftFormTemplate template = DraftFormTemplate.create(
                root, 1, name, workType, "{}", true,
                ChangeAction.CREATE, null, "user", "user", now);
        root.activateNewVersion(template, now);
        return template;
    }

    @Test
    @DisplayName("workType 필터가 적용되어 해당 업무유형의 템플릿만 반환된다")
    void listSummary_filtersByWorkType() {
        AuthContext ctx = AuthContext.of("user", "ORG", "PG", null, null, List.of());
        AuthContextHolder.set(ctx);

        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplate t1 = createPublishedTemplate("name1", WorkType.HR_UPDATE, now);
        given(service.listDraftFormTemplateSummaries(WorkType.HR_UPDATE)).willReturn(List.of(t1));

        var result = controller.listSummary(WorkType.HR_UPDATE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).workType()).isEqualTo(WorkType.HR_UPDATE);
    }

    @Test
    @DisplayName("workType이 null이면 모든 활성 템플릿을 반환한다")
    void listSummary_returnsAllWhenNoFilter() {
        AuthContext ctx = AuthContext.of("user", "ORG", "PG", null, null, List.of());
        AuthContextHolder.set(ctx);

        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplate t1 = createPublishedTemplate("name1", WorkType.HR_UPDATE, now);
        DraftFormTemplate t2 = createPublishedTemplate("name2", WorkType.GENERAL, now);
        given(service.listDraftFormTemplateSummaries(null)).willReturn(List.of(t1, t2));

        var result = controller.listSummary(null);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("반환된 응답에는 name, workType, version이 포함된다")
    void listSummary_returnsSummaryWithExpectedFields() {
        AuthContext ctx = AuthContext.of("user", "ORG", "PG", null, null, List.of());
        AuthContextHolder.set(ctx);

        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplate t1 = createPublishedTemplate("폼템플릿", WorkType.GENERAL, now);
        given(service.listDraftFormTemplateSummaries(null)).willReturn(List.of(t1));

        var result = controller.listSummary(null);

        assertThat(result).hasSize(1);
        var summary = result.get(0);
        assertThat(summary.name()).isEqualTo("폼템플릿");
        assertThat(summary.workType()).isEqualTo(WorkType.GENERAL);
        assertThat(summary.version()).isEqualTo(1);
        assertThat(summary.active()).isTrue();
    }
}
