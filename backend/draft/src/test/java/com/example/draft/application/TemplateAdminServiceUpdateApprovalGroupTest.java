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

import com.example.auth.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.draft.application.request.ApprovalGroupRequest;
import com.example.draft.application.response.ApprovalGroupResponse;
import com.example.approval.domain.ApprovalGroup;
import com.example.approval.domain.repository.ApprovalGroupRepository;
import com.example.approval.domain.repository.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class TemplateAdminServiceUpdateApprovalGroupTest {

    @Test
    @DisplayName("동일 조직이면 updateApprovalGroup이 성공적으로 값을 갱신한다")
    void updateApprovalGroupSuccess() {
        ApprovalGroupRepository groupRepo = mock(ApprovalGroupRepository.class);
        TemplateAdminService service = new TemplateAdminService(
                groupRepo,
                mock(ApprovalLineTemplateRepository.class),
                mock(DraftFormTemplateRepository.class), mock(com.example.draft.domain.repository.DraftTemplatePresetRepository.class), new com.fasterxml.jackson.databind.ObjectMapper());

        ApprovalGroup group = ApprovalGroup.create("G1", "old", "desc", "ORG1", null, OffsetDateTime.now());
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000021");
        given(groupRepo.findById(id)).willReturn(Optional.of(group));
        given(groupRepo.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        ApprovalGroupRequest req = new ApprovalGroupRequest("G1", "newName", "newDesc", "ORG1", "expr");
        AuthContext ctx = new AuthContext("u", "ORG1", null, null, null, RowScope.ORG, null);

        ApprovalGroupResponse res = service.updateApprovalGroup(id, req, ctx, false);

        assertThat(res.name()).isEqualTo("newName");
        assertThat(res.organizationCode()).isEqualTo("ORG1");
    }
}
