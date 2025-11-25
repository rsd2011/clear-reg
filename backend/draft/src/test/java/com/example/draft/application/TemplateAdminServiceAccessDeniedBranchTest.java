package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.auth.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.draft.application.request.ApprovalGroupRequest;
import com.example.approval.domain.ApprovalGroup;
import com.example.draft.domain.exception.DraftAccessDeniedException;
import com.example.approval.domain.repository.ApprovalGroupRepository;
import com.example.approval.domain.repository.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class TemplateAdminServiceAccessDeniedBranchTest {

    @Test
    @DisplayName("audit=false이고 다른 조직이면 접근을 거부한다")
    void deniesWhenOrgMismatch() {
        ApprovalGroupRepository groupRepo = mock(ApprovalGroupRepository.class);
        ApprovalLineTemplateRepository lineRepo = mock(ApprovalLineTemplateRepository.class);
        DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
        TemplateAdminService service = new TemplateAdminService(groupRepo, lineRepo, formRepo, mock(com.example.draft.domain.repository.DraftTemplatePresetRepository.class), new com.fasterxml.jackson.databind.ObjectMapper());

        ApprovalGroup group = ApprovalGroup.create("G1", "n", null, "ORG1", null, OffsetDateTime.now());
        given(groupRepo.findById(UUID.fromString("00000000-0000-0000-0000-000000000001"))).willReturn(Optional.of(group));

        AuthContext ctx = new AuthContext("u", "ORG2", null, null, null, RowScope.ORG, null);
        ApprovalGroupRequest req = new ApprovalGroupRequest("G1", "n2", "desc", "ORG1", null);

        assertThatThrownBy(() -> service.updateApprovalGroup(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                req,
                ctx,
                false
        )).isInstanceOf(DraftAccessDeniedException.class);
    }
}
