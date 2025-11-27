package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.example.admin.approval.dto.ApprovalGroupRequest;
import com.example.admin.approval.dto.ApprovalGroupResponse;
import com.example.admin.approval.ApprovalGroup;
import com.example.admin.approval.ApprovalGroupRepository;
import com.example.admin.approval.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class TemplateAdminServiceAccessTest {

    @Test
    @DisplayName("결재 그룹 업데이트가 성공한다")
    void updateApprovalGroup_success() {
        ApprovalGroupRepository groupRepo = mock(ApprovalGroupRepository.class);
        ApprovalLineTemplateRepository lineRepo = mock(ApprovalLineTemplateRepository.class);
        DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
        TemplateAdminService service = new TemplateAdminService(groupRepo, lineRepo, formRepo, mock(com.example.draft.domain.repository.DraftTemplatePresetRepository.class), new com.fasterxml.jackson.databind.ObjectMapper());

        ApprovalGroup group = ApprovalGroup.create("GC", "name", "desc", 0, OffsetDateTime.now());
        UUID id = UUID.randomUUID();
        given(groupRepo.findById(any())).willReturn(Optional.of(group));

        AuthContext ctx = AuthContext.of("u", "ORG-1", null, null, null, RowScope.ORG);
        ApprovalGroupRequest request = new ApprovalGroupRequest("GC", "new-name", null, 5);

        ApprovalGroupResponse res = service.updateApprovalGroup(id, request, ctx, false);

        assertThat(res.name()).isEqualTo("new-name");
        assertThat(res.priority()).isEqualTo(5);
    }
}
