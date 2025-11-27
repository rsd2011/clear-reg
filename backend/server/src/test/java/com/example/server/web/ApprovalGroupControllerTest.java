package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.approval.ApprovalGroupService;
import com.example.admin.approval.dto.ApprovalGroupRequest;
import com.example.admin.approval.dto.ApprovalGroupResponse;
import com.example.admin.approval.dto.ApprovalGroupUpdateRequest;
import com.example.admin.approval.dto.ApprovalGroupPriorityRequest;
import com.example.admin.approval.dto.GroupCodeExistsResponse;
import com.example.admin.permission.PermissionDeniedException;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.security.RowScope;

class ApprovalGroupControllerTest {

    ApprovalGroupService service = mock(ApprovalGroupService.class);
    ApprovalGroupController controller = new ApprovalGroupController(service);

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("인증 컨텍스트가 없으면 PermissionDeniedException을 던진다")
    void throwsWhenContextMissing() {
        AuthContextHolder.clear();

        assertThatThrownBy(() -> controller.listGroups(null, true))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    @DisplayName("승인 그룹 생성 시 컨텍스트와 true 플래그가 전달된다")
    void createGroupUsesContext() {
        AuthContext ctx = AuthContext.of("user", "ORG", "PG", null, null, RowScope.ALL);
        AuthContextHolder.set(ctx);

        ApprovalGroupRequest request = new ApprovalGroupRequest("GC", "이름", "설명", 0);
        ApprovalGroupResponse response = new ApprovalGroupResponse(
                UUID.randomUUID(),
                "GC",
                "이름",
                "설명",
                0,
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now());
        when(service.createApprovalGroup(eq(request), eq(ctx), eq(true))).thenReturn(response);

        ApprovalGroupResponse result = controller.createGroup(request);

        assertThat(result.groupCode()).isEqualTo(response.groupCode());
        assertThat(result.name()).isEqualTo(response.name());
        verify(service).createApprovalGroup(eq(request), eq(ctx), eq(true));
    }

    @Test
    @DisplayName("승인 그룹 목록 조회 시 컨텍스트와 파라미터가 전달된다")
    void listGroupsPassesArguments() {
        AuthContext ctx = AuthContext.of("user", "ORG", "PG", null, null, RowScope.ALL);
        AuthContextHolder.set(ctx);

        ApprovalGroupResponse response = new ApprovalGroupResponse(
                UUID.randomUUID(),
                "GC",
                "name",
                "desc",
                0,
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now());
        when(service.listApprovalGroups(any(), anyBoolean(), any(), anyBoolean())).thenReturn(List.of(response));

        List<ApprovalGroupResponse> result = controller.listGroups("keyword", false);

        assertThat(result).hasSize(1);
        verify(service).listApprovalGroups(eq("keyword"), eq(false), eq(ctx), eq(true));
    }

    @Test
    @DisplayName("승인 그룹 상세 조회 시 ID가 전달된다")
    void getGroupUsesId() {
        AuthContextHolder.set(AuthContext.of("user", "ORG", null, null, null, RowScope.ALL));

        UUID id = UUID.randomUUID();
        ApprovalGroupResponse response = new ApprovalGroupResponse(
                id,
                "GC",
                "name",
                "desc",
                0,
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now());
        when(service.getApprovalGroup(id)).thenReturn(response);

        ApprovalGroupResponse result = controller.getGroup(id);

        assertThat(result.id()).isEqualTo(id);
        verify(service).getApprovalGroup(id);
    }

    @Test
    @DisplayName("승인 그룹 수정 시 컨텍스트와 요청이 전달된다")
    void updateGroupUsesContext() {
        AuthContext ctx = AuthContext.of("user", "ORG", null, null, null, RowScope.ALL);
        AuthContextHolder.set(ctx);

        UUID id = UUID.randomUUID();
        ApprovalGroupUpdateRequest request = new ApprovalGroupUpdateRequest("newName", "newDesc", 1);
        ApprovalGroupResponse response = new ApprovalGroupResponse(
                id,
                "GC",
                "newName",
                "newDesc",
                1,
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now());
        when(service.updateApprovalGroup(eq(id), eq(request), eq(ctx), eq(true))).thenReturn(response);

        ApprovalGroupResponse result = controller.updateGroup(id, request);

        assertThat(result.name()).isEqualTo("newName");
        verify(service).updateApprovalGroup(eq(id), eq(request), eq(ctx), eq(true));
    }

    @Test
    @DisplayName("승인 그룹 삭제 시 컨텍스트가 전달된다")
    void deleteGroupUsesContext() {
        AuthContext ctx = AuthContext.of("user", "ORG", null, null, null, RowScope.ALL);
        AuthContextHolder.set(ctx);

        UUID id = UUID.randomUUID();
        controller.deleteGroup(id);

        verify(service).deleteApprovalGroup(eq(id), eq(ctx), eq(true));
    }

    @Test
    @DisplayName("승인 그룹 활성화 시 컨텍스트가 전달된다")
    void activateGroupUsesContext() {
        AuthContext ctx = AuthContext.of("user", "ORG", null, null, null, RowScope.ALL);
        AuthContextHolder.set(ctx);

        UUID id = UUID.randomUUID();
        ApprovalGroupResponse response = new ApprovalGroupResponse(
                id,
                "GC",
                "name",
                "desc",
                0,
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now());
        when(service.activateApprovalGroup(eq(id), eq(ctx), eq(true))).thenReturn(response);

        ApprovalGroupResponse result = controller.activateGroup(id);

        assertThat(result.active()).isTrue();
        verify(service).activateApprovalGroup(eq(id), eq(ctx), eq(true));
    }

    @Test
    @DisplayName("그룹 코드 중복 검사 결과를 반환한다")
    void checkGroupCodeExistsReturnsResult() {
        AuthContextHolder.set(AuthContext.of("user", "ORG", null, null, null, RowScope.ALL));

        when(service.existsGroupCode("GC")).thenReturn(true);

        GroupCodeExistsResponse result = controller.checkGroupCodeExists("GC");

        assertThat(result.exists()).isTrue();
        verify(service).existsGroupCode("GC");
    }

    @Test
    @DisplayName("우선순위 일괄 업데이트 시 컨텍스트가 전달된다")
    void updatePrioritiesUsesContext() {
        AuthContext ctx = AuthContext.of("user", "ORG", null, null, null, RowScope.ALL);
        AuthContextHolder.set(ctx);

        UUID id = UUID.randomUUID();
        ApprovalGroupPriorityRequest request = new ApprovalGroupPriorityRequest(
                List.of(new ApprovalGroupPriorityRequest.PriorityItem(id, 5)));
        ApprovalGroupResponse response = new ApprovalGroupResponse(
                id,
                "GC",
                "name",
                "desc",
                5,
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now());
        when(service.updateApprovalGroupPriorities(eq(request), eq(ctx), eq(true))).thenReturn(List.of(response));

        List<ApprovalGroupResponse> result = controller.updateGroupPriorities(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).priority()).isEqualTo(5);
        verify(service).updateApprovalGroupPriorities(eq(request), eq(ctx), eq(true));
    }
}
