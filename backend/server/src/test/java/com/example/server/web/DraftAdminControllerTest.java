package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.draft.domain.DraftFormTemplate;
import com.example.draft.domain.TemplateScope;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class DraftAdminControllerTest {

    DraftFormTemplateRepository formRepo = Mockito.mock(DraftFormTemplateRepository.class);
    DraftAdminController controller = new DraftAdminController(formRepo);

    @Test
    @DisplayName("businessType 필터가 null이면 모든 폼 템플릿을 반환한다")
    void listFormTemplates_noFilter_returnsAll() {
        DraftFormTemplate t1 = DraftFormTemplate.create("폼1", "HR", "ORG1", "{}", java.time.OffsetDateTime.now());
        DraftFormTemplate t2 = DraftFormTemplate.create("폼2", "IT", null, "{}", java.time.OffsetDateTime.now());
        given(formRepo.findAll()).willReturn(List.of(t1, t2));

        var result = controller.listFormTemplates(null);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("businessType 필터가 적용되면 해당 비즈니스 타입만 반환한다")
    void listFormTemplates_filtersByBusinessType() {
        DraftFormTemplate t1 = DraftFormTemplate.create("폼1", "HR", "ORG1", "{}", java.time.OffsetDateTime.now());
        DraftFormTemplate t2 = DraftFormTemplate.create("폼2", "IT", null, "{}", java.time.OffsetDateTime.now());
        given(formRepo.findAll()).willReturn(List.of(t1, t2));

        var result = controller.listFormTemplates("HR");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().businessType()).isEqualTo("HR");
    }
}
