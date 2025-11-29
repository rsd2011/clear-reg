package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.policy.DataPolicyContextHolder;
import com.example.common.policy.DataPolicyMatch;
import com.example.common.security.RowScope;
import com.example.common.security.RowScopeContext;
import com.example.common.security.RowScopeContextHolder;
import com.example.draft.domain.DraftFormTemplate;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class DraftAdminControllerBranchTest {

    DraftFormTemplateRepository repository = org.mockito.Mockito.mock(DraftFormTemplateRepository.class);
    DraftAdminController controller = new DraftAdminController(repository);

    @AfterEach
    void tearDown() {
        DataPolicyContextHolder.clear();
        RowScopeContextHolder.clear();
    }

    @Test
    @DisplayName("businessType 필터가 적용되고 기본 RowScope.ALL이면 모든 조직을 허용한다")
    void listTemplates_filtersByBusinessType_AllScope() {
        DraftFormTemplate t1 = DraftFormTemplate.create("name1", "HR", null, "{}", OffsetDateTime.now());
        DraftFormTemplate t2 = DraftFormTemplate.create("name2", "FIN", "ORG1", "{}", OffsetDateTime.now());
        given(repository.findAll()).willReturn(List.of(t1, t2));

        var result = controller.listFormTemplates("HR");

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("RowScope.ORГ이면 조직 계층에 포함된 템플릿만 반환한다")
    void listTemplates_filtersByOrgScope() {
        DataPolicyContextHolder.set(DataPolicyMatch.builder().rowScope(RowScope.ORG).build());
        RowScopeContextHolder.set(new RowScopeContext("ORG1", List.of("ORG1", "ORG_CHILD")));

        DraftFormTemplate org1 = DraftFormTemplate.create("name1", "HR", "ORG1", "{}", OffsetDateTime.now());
        DraftFormTemplate org2 = DraftFormTemplate.create("name2", "HR", "ORG2", "{}", OffsetDateTime.now());
        given(repository.findAll()).willReturn(List.of(org1, org2));

        var result = controller.listFormTemplates(null);

        assertThat(result).extracting(DraftAdminController.DraftFormTemplateSummary::organizationCode)
                .containsExactly("ORG1");
    }

    @Test
    @DisplayName("RowScope.OWN이면 단일 조직 코드가 없으면 필터에 걸린다")
    void listTemplates_filtersByOwnScope() {
        DataPolicyContextHolder.set(DataPolicyMatch.builder().rowScope(RowScope.OWN).build());
        RowScopeContextHolder.set(new RowScopeContext("ORG1", List.of()));

        DraftFormTemplate org1 = DraftFormTemplate.create("name1", "HR", "ORG1", "{}", OffsetDateTime.now());
        DraftFormTemplate orgNull = DraftFormTemplate.create("name2", "HR", null, "{}", OffsetDateTime.now());
        given(repository.findAll()).willReturn(List.of(org1, orgNull));

        var result = controller.listFormTemplates(null);

        assertThat(result).extracting(DraftAdminController.DraftFormTemplateSummary::organizationCode)
                .containsExactly("ORG1");
    }
}

