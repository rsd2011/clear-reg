package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.example.draft.domain.ApprovalGroup;
import com.example.draft.domain.repository.ApprovalGroupRepository;
import com.example.draft.domain.repository.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class TemplateAdminServiceRowScopeAllTest {

    @Test
    @DisplayName("RowScope.ALL이면 다른 조직 그룹도 업데이트를 허용한다")
    void allowsOtherOrgWhenRowScopeAll() {
        ApprovalGroupRepository groupRepo = mock(ApprovalGroupRepository.class);
        ApprovalLineTemplateRepository lineRepo = mock(ApprovalLineTemplateRepository.class);
        DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
        TemplateAdminService service = new TemplateAdminService(groupRepo, lineRepo, formRepo);

        ApprovalGroup group = ApprovalGroup.create("G1", "n", null, "ORG1", null, OffsetDateTime.now());
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000002");
        given(groupRepo.findById(id)).willReturn(Optional.of(group));

        AuthContext ctx = new AuthContext("u", "ORG2", null, null, null, RowScope.ALL, null);
        ApprovalGroupRequest req = new ApprovalGroupRequest("G1", "renamed", "desc", "ORG1", null);

        ApprovalGroupResponse res = service.updateApprovalGroup(id, req, ctx, false);

        assertThat(res.name()).isEqualTo("renamed");
    }
}
