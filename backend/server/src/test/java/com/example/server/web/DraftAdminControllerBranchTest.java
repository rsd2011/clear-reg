package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.orggroup.WorkType;
import com.example.common.version.ChangeAction;
import com.example.admin.draft.domain.DraftFormTemplate;
import com.example.admin.draft.domain.DraftFormTemplateRoot;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class DraftAdminControllerBranchTest {

    DraftFormTemplateRepository repository = org.mockito.Mockito.mock(DraftFormTemplateRepository.class);
    DraftAdminController controller = new DraftAdminController(repository);

    private DraftFormTemplate createPublishedTemplate(String name, WorkType workType, OffsetDateTime now) {
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);
        DraftFormTemplate template = DraftFormTemplate.create(
                root, 1, name, workType, "{}", true,
                ChangeAction.CREATE, null, "user", "user", now);
        // create()는 이미 PUBLISHED 상태로 생성됨
        root.activateNewVersion(template, now);
        return template;
    }

    @Test
    @DisplayName("workType 필터가 적용되어 해당 업무유형의 템플릿만 반환된다")
    void listTemplates_filtersByWorkType() {
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplate t1 = createPublishedTemplate("name1", WorkType.HR_UPDATE, now);
        given(repository.findCurrentByWorkType(WorkType.HR_UPDATE)).willReturn(List.of(t1));

        var result = controller.listFormTemplates(WorkType.HR_UPDATE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).workType()).isEqualTo(WorkType.HR_UPDATE);
    }

    @Test
    @DisplayName("workType이 null이면 모든 활성 템플릿을 반환한다")
    void listTemplates_returnsAllWhenNoFilter() {
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplate t1 = createPublishedTemplate("name1", WorkType.HR_UPDATE, now);
        DraftFormTemplate t2 = createPublishedTemplate("name2", WorkType.GENERAL, now);
        given(repository.findAllCurrent()).willReturn(List.of(t1, t2));

        var result = controller.listFormTemplates(null);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("반환된 응답에는 templateCode, name, workType, version이 포함된다")
    void listTemplates_returnsSummaryWithExpectedFields() {
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplate t1 = createPublishedTemplate("폼템플릿", WorkType.GENERAL, now);
        given(repository.findAllCurrent()).willReturn(List.of(t1));

        var result = controller.listFormTemplates(null);

        assertThat(result).hasSize(1);
        var summary = result.get(0);
        assertThat(summary.name()).isEqualTo("폼템플릿");
        assertThat(summary.workType()).isEqualTo(WorkType.GENERAL);
        assertThat(summary.version()).isEqualTo(1);
        assertThat(summary.active()).isTrue();
    }
}
