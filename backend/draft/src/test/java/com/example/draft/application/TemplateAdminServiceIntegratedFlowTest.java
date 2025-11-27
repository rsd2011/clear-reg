package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.List;
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

class TemplateAdminServiceIntegratedFlowTest {

    @Test
    @DisplayName("결재 그룹 생성 후 이름을 변경하고 리스트에서 변경된 값이 조회된다")
    void createUpdateAndList() {
        ApprovalGroupRepository groupRepo = mock(ApprovalGroupRepository.class);
        TemplateAdminService service = new TemplateAdminService(
                groupRepo,
                mock(ApprovalLineTemplateRepository.class),
                mock(DraftFormTemplateRepository.class), mock(com.example.draft.domain.repository.DraftTemplatePresetRepository.class), new com.fasterxml.jackson.databind.ObjectMapper());

        // create
        ApprovalGroupRequest createReq = new ApprovalGroupRequest("G1", "name", "desc", 0);
        AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, RowScope.ORG);
        given(groupRepo.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        ApprovalGroupResponse created = service.createApprovalGroup(createReq, ctx, false);

        // update
        ApprovalGroup stored = ApprovalGroup.create("G1", "name", "desc", 0, OffsetDateTime.now());
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000001111");
        given(groupRepo.findById(id)).willReturn(Optional.of(stored));
        ApprovalGroupRequest updateReq = new ApprovalGroupRequest("G1", "new-name", "new-desc", 5);
        ApprovalGroupResponse updated = service.updateApprovalGroup(id, updateReq, ctx, false);

        // list
        given(groupRepo.findAll()).willReturn(List.of(stored));
        List<ApprovalGroupResponse> list = service.listApprovalGroups(null, ctx, false);

        assertThat(created.groupCode()).isEqualTo("G1");
        assertThat(updated.name()).isEqualTo("new-name");
        assertThat(list).extracting(ApprovalGroupResponse::name).containsExactly("new-name");
    }
}
