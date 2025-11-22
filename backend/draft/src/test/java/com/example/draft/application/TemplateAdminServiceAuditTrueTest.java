package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.auth.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.draft.application.response.ApprovalGroupResponse;
import com.example.draft.domain.ApprovalGroup;
import com.example.draft.domain.repository.ApprovalGroupRepository;
import com.example.draft.domain.repository.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class TemplateAdminServiceAuditTrueTest {

    @Test
    @DisplayName("audit=true이면 조직 필터 없이 모든 그룹을 반환한다")
    void auditTrueReturnsAllGroups() {
        ApprovalGroupRepository groupRepo = mock(ApprovalGroupRepository.class);
        ApprovalLineTemplateRepository lineRepo = mock(ApprovalLineTemplateRepository.class);
        DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
        TemplateAdminService service = new TemplateAdminService(groupRepo, lineRepo, formRepo);

        ApprovalGroup org1 = ApprovalGroup.create("G1", "name", null, "ORG1", null, OffsetDateTime.now());
        ApprovalGroup org2 = ApprovalGroup.create("G2", "name", null, "ORG2", null, OffsetDateTime.now());
        given(groupRepo.findAll()).willReturn(List.of(org1, org2));

        AuthContext ctx = new AuthContext("u", "ORG1", null, null, null, RowScope.ORG, null);

        List<ApprovalGroupResponse> result = service.listApprovalGroups(null, ctx, true);

        assertThat(result).extracting(ApprovalGroupResponse::organizationCode).containsExactlyInAnyOrder("ORG1", "ORG2");
    }
}
