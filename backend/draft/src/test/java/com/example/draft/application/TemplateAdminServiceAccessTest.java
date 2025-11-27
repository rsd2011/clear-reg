package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.context.AuthContext;
import com.example.common.security.RowScope;
import static org.mockito.Mockito.when;
import com.example.admin.approval.dto.ApprovalGroupRequest;
import com.example.admin.approval.ApprovalGroup;
import com.example.draft.domain.exception.DraftAccessDeniedException;
import com.example.admin.approval.ApprovalGroupRepository;
import com.example.admin.approval.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class TemplateAdminServiceAccessTest {

    @Test
    @DisplayName("조직이 다르면 결재 그룹 업데이트를 거부한다")
    void updateApprovalGroup_deniesOtherOrg() {
        ApprovalGroupRepository groupRepo = mock(ApprovalGroupRepository.class);
        ApprovalLineTemplateRepository lineRepo = mock(ApprovalLineTemplateRepository.class);
        DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
        TemplateAdminService service = new TemplateAdminService(groupRepo, lineRepo, formRepo, mock(com.example.draft.domain.repository.DraftTemplatePresetRepository.class), new com.fasterxml.jackson.databind.ObjectMapper());

        ApprovalGroup group = ApprovalGroup.create("GC", "name", "desc", "ORG-1", null, OffsetDateTime.now());
        given(groupRepo.findById(any())).willReturn(Optional.of(group));

        AuthContext ctx = mock(AuthContext.class);
        when(ctx.organizationCode()).thenReturn("ORG-2");
        when(ctx.rowScope()).thenReturn(RowScope.ORG);
        ApprovalGroupRequest request = new ApprovalGroupRequest("GC", "name", null, "ORG-1", "cond");

        assertThatThrownBy(() -> service.updateApprovalGroup(UUID.randomUUID(), request, ctx, false))
                .isInstanceOf(DraftAccessDeniedException.class);
    }
}
