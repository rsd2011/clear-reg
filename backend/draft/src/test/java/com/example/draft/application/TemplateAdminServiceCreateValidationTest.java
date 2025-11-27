package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.admin.approval.dto.ApprovalGroupRequest;
import com.example.admin.approval.ApprovalGroupRepository;
import com.example.admin.approval.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class TemplateAdminServiceCreateValidationTest {

    @Test
    @DisplayName("organizationCode가 비어 있으면 createApprovalGroup에서 예외를 던진다")
    void createApprovalGroup_requiresOrg() {
        TemplateAdminService service = new TemplateAdminService(
                mock(ApprovalGroupRepository.class),
                mock(ApprovalLineTemplateRepository.class),
                mock(DraftFormTemplateRepository.class), mock(com.example.draft.domain.repository.DraftTemplatePresetRepository.class), new com.fasterxml.jackson.databind.ObjectMapper());

        ApprovalGroupRequest req = new ApprovalGroupRequest("G1", "n", "d", " ", null);
        AuthContext ctx = new AuthContext("u", "ORG1", null, null, null, RowScope.ORG, null);

        assertThatThrownBy(() -> service.createApprovalGroup(req, ctx, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
