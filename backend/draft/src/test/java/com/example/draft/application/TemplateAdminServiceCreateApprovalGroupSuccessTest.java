package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.auth.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.draft.application.request.ApprovalGroupRequest;
import com.example.draft.application.response.ApprovalGroupResponse;
import com.example.draft.domain.ApprovalGroup;
import com.example.draft.domain.repository.ApprovalGroupRepository;
import com.example.draft.domain.repository.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class TemplateAdminServiceCreateApprovalGroupSuccessTest {

    @Test
    @DisplayName("approvalGroup 생성 성공 시 저장된 엔티티를 반환한다")
    void createApprovalGroupSuccess() {
        ApprovalGroupRepository groupRepo = mock(ApprovalGroupRepository.class);
        TemplateAdminService service = new TemplateAdminService(
                groupRepo,
                mock(ApprovalLineTemplateRepository.class),
                mock(DraftFormTemplateRepository.class));

        given(groupRepo.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        ApprovalGroupRequest req = new ApprovalGroupRequest("G1", "name", "desc", "ORG1", "expr");
        AuthContext ctx = new AuthContext("u", "ORG1", null, null, null, RowScope.ORG, null);

        ApprovalGroupResponse res = service.createApprovalGroup(req, ctx, false);

        assertThat(res.groupCode()).isEqualTo("G1");
        assertThat(res.name()).isEqualTo("name");
    }
}
