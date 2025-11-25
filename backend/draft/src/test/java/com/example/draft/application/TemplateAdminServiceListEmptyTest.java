package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.auth.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.draft.application.response.ApprovalGroupResponse;
import com.example.approval.domain.repository.ApprovalGroupRepository;
import com.example.approval.domain.repository.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class TemplateAdminServiceListEmptyTest {

    @Test
    @DisplayName("결재 그룹이 없으면 빈 리스트를 반환한다")
    void returnsEmptyListWhenNoGroups() {
        ApprovalGroupRepository groupRepo = mock(ApprovalGroupRepository.class);
        given(groupRepo.findAll()).willReturn(List.of());
        TemplateAdminService service = new TemplateAdminService(
                groupRepo,
                mock(ApprovalLineTemplateRepository.class),
                mock(DraftFormTemplateRepository.class), mock(com.example.draft.domain.repository.DraftTemplatePresetRepository.class), new com.fasterxml.jackson.databind.ObjectMapper());

        AuthContext ctx = new AuthContext("u", "ORG1", null, null, null, RowScope.ORG, null);

        List<ApprovalGroupResponse> result = service.listApprovalGroups(null, ctx, false);

        assertThat(result).isEmpty();
    }
}
