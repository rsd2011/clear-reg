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

import com.example.admin.approval.service.ApprovalGroupService;
import com.example.admin.approval.dto.DisplayOrderUpdateRequest;
import com.example.admin.approval.dto.ApprovalGroupRequest;
import com.example.admin.approval.dto.ApprovalGroupResponse;
import com.example.admin.approval.dto.ApprovalGroupSummaryResponse;
import com.example.admin.approval.dto.ApprovalGroupUpdateRequest;

import com.example.admin.permission.exception.PermissionDeniedException;
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
        AuthContext ctx = AuthContext.of("user", "ORG", "PG", null, null, List.of());
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
        AuthContext ctx = AuthContext.of("user", "ORG", "PG", null, null, List.of());
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
        AuthContextHolder.set(AuthContext.of("user", "ORG", null, null, null, List.of()));

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
        AuthContext ctx = AuthContext.of("user", "ORG", null, null, null, List.of());
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
        AuthContext ctx = AuthContext.of("user", "ORG", null, null, null, List.of());
        AuthContextHolder.set(ctx);

        UUID id = UUID.randomUUID();
        controller.deleteGroup(id);

        verify(service).deleteApprovalGroup(eq(id), eq(ctx), eq(true));
    }

    @Test
    @DisplayName("승인 그룹 활성화 시 컨텍스트가 전달된다")
    void activateGroupUsesContext() {
        AuthContext ctx = AuthContext.of("user", "ORG", null, null, null, List.of());
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
        AuthContextHolder.set(AuthContext.of("user", "ORG", null, null, null, List.of()));

        when(service.existsGroupCode("GC")).thenReturn(true);

        boolean result = controller.checkGroupCodeExists("GC");

        assertThat(result).isTrue();
        verify(service).existsGroupCode("GC");
    }

    @Test
    @DisplayName("표시순서 일괄 업데이트 시 컨텍스트가 전달된다")
    void updateDisplayOrdersUsesContext() {
        AuthContext ctx = AuthContext.of("user", "ORG", null, null, null, List.of());
        AuthContextHolder.set(ctx);

        UUID id = UUID.randomUUID();
        DisplayOrderUpdateRequest request = new DisplayOrderUpdateRequest(
                List.of(new DisplayOrderUpdateRequest.DisplayOrderItem(id, 5)));
        ApprovalGroupResponse response = new ApprovalGroupResponse(
                id,
                "GC",
                "name",
                "desc",
                5,
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now());
        when(service.updateApprovalGroupDisplayOrders(eq(request), eq(ctx), eq(true))).thenReturn(List.of(response));

        List<ApprovalGroupResponse> result = controller.updateGroupDisplayOrders(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).displayOrder()).isEqualTo(5);
        verify(service).updateApprovalGroupDisplayOrders(eq(request), eq(ctx), eq(true));
    }

    @Test
    @DisplayName("승인그룹 요약 목록 조회 시 경량 응답이 반환된다")
    void listGroupSummaryReturnsLightweightResponse() {
        AuthContextHolder.set(AuthContext.of("user", "ORG", "PG", null, null, List.of()));

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        ApprovalGroupSummaryResponse s1 = new ApprovalGroupSummaryResponse(id1, "TEAM_LEADER", "팀장");
        ApprovalGroupSummaryResponse s2 = new ApprovalGroupSummaryResponse(id2, "DEPT_HEAD", "부서장");

        when(service.listGroupSummary(true)).thenReturn(List.of(s1, s2));

        List<ApprovalGroupSummaryResponse> result = controller.listGroupSummary(true);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ApprovalGroupSummaryResponse::groupCode)
                .containsExactlyInAnyOrder("TEAM_LEADER", "DEPT_HEAD");
        verify(service).listGroupSummary(eq(true));
    }

    @Test
    @DisplayName("승인그룹 요약 목록 - activeOnly=false 시 모든 그룹 반환 요청")
    void listGroupSummaryIncludesInactive() {
        AuthContextHolder.set(AuthContext.of("user", "ORG", "PG", null, null, List.of()));

        when(service.listGroupSummary(false)).thenReturn(List.of(
                new ApprovalGroupSummaryResponse(UUID.randomUUID(), "ACTIVE", "활성"),
                new ApprovalGroupSummaryResponse(UUID.randomUUID(), "INACTIVE", "비활성")));

        List<ApprovalGroupSummaryResponse> result = controller.listGroupSummary(false);

        assertThat(result).hasSize(2);
        verify(service).listGroupSummary(eq(false));
    }
}
