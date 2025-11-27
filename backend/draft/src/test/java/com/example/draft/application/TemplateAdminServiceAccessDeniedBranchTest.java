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

class TemplateAdminServiceAccessDeniedBranchTest {

    @Test
    @DisplayName("audit=false일 때도 업데이트가 정상 동작한다")
    void updateWorksWithoutAudit() {
        ApprovalGroupRepository groupRepo = mock(ApprovalGroupRepository.class);
        ApprovalLineTemplateRepository lineRepo = mock(ApprovalLineTemplateRepository.class);
        DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
        TemplateAdminService service = new TemplateAdminService(groupRepo, lineRepo, formRepo, mock(com.example.draft.domain.repository.DraftTemplatePresetRepository.class), new com.fasterxml.jackson.databind.ObjectMapper());

        ApprovalGroup group = ApprovalGroup.create("G1", "n", null, 0, OffsetDateTime.now());
        given(groupRepo.findById(UUID.fromString("00000000-0000-0000-0000-000000000001"))).willReturn(Optional.of(group));

        AuthContext ctx = AuthContext.of("u", "ORG2", null, null, null, RowScope.ORG);
        ApprovalGroupRequest req = new ApprovalGroupRequest("G1", "n2", "desc", 5);

        ApprovalGroupResponse res = service.updateApprovalGroup(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                req,
                ctx,
                false
        );

        assertThat(res.name()).isEqualTo("n2");
        assertThat(res.priority()).isEqualTo(5);
    }
}
