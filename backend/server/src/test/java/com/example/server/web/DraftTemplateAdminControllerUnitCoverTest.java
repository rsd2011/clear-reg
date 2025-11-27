package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.security.RowScope;
import com.example.draft.application.TemplateAdminService;
import com.example.admin.approval.dto.ApprovalGroupResponse;

class DraftTemplateAdminControllerUnitCoverTest {

    TemplateAdminService service = Mockito.mock(TemplateAdminService.class);
    DraftTemplateAdminController controller = new DraftTemplateAdminController(service);

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("listGroups는 현재 컨텍스트를 전달해 서비스 결과를 반환한다")
    void listGroupsReturnsServiceResult() {
        AuthContextHolder.set(AuthContext.of("u", "ORG", null, null, null, RowScope.ALL));
        ApprovalGroupResponse resp = new ApprovalGroupResponse(
                UUID.randomUUID(),
                "g",
                "name",
                "desc",
                "ORG",
                "expr",
                OffsetDateTime.now(),
                OffsetDateTime.now());
        when(service.listApprovalGroups(any(), any(), anyBoolean())).thenReturn(List.of(resp));

        List<ApprovalGroupResponse> result = controller.listGroups("ORG");

        assertThat(result).containsExactly(resp);
    }
}
