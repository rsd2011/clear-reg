package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
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

class TemplateAdminServiceRowScopeAllTest {

    @Test
    @DisplayName("RowScope.ALL이면 그룹 업데이트를 허용한다")
    void allowsUpdateWhenRowScopeAll() {
        ApprovalGroupRepository groupRepo = mock(ApprovalGroupRepository.class);
        ApprovalLineTemplateRepository lineRepo = mock(ApprovalLineTemplateRepository.class);
        DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
        TemplateAdminService service = new TemplateAdminService(groupRepo, lineRepo, formRepo, mock(com.example.draft.domain.repository.DraftTemplatePresetRepository.class), new com.fasterxml.jackson.databind.ObjectMapper());

        ApprovalGroup group = ApprovalGroup.create("G1", "n", null, 0, OffsetDateTime.now());
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000002");
        given(groupRepo.findById(id)).willReturn(Optional.of(group));

        AuthContext ctx = AuthContext.of("u", "ORG2", null, null, null, RowScope.ALL);
        ApprovalGroupRequest req = new ApprovalGroupRequest("G1", "renamed", "desc", 5);

        ApprovalGroupResponse res = service.updateApprovalGroup(id, req, ctx, false);

        assertThat(res.name()).isEqualTo("renamed");
        assertThat(res.priority()).isEqualTo(5);
    }
}
