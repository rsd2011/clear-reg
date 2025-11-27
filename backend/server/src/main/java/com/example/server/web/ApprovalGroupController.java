package com.example.server.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import com.example.admin.approval.ApprovalGroupService;
import com.example.admin.approval.dto.ApprovalGroupPriorityRequest;
import com.example.admin.approval.dto.ApprovalGroupRequest;
import com.example.admin.approval.dto.ApprovalGroupResponse;
import com.example.admin.approval.dto.ApprovalGroupUpdateRequest;
import com.example.admin.approval.dto.GroupCodeExistsResponse;
import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.PermissionDeniedException;
import com.example.admin.permission.RequirePermission;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.masking.MaskingFunctions;
import com.example.common.policy.DataPolicyContextHolder;

@RestController
@Validated
@RequestMapping("/api/admin/approval-groups")
@Tag(name = "Approval Group Admin", description = "승인그룹 관리 API")
public class ApprovalGroupController {

    private final ApprovalGroupService approvalGroupService;

    public ApprovalGroupController(ApprovalGroupService approvalGroupService) {
        this.approvalGroupService = approvalGroupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    @Operation(summary = "승인그룹 등록")
    public ApprovalGroupResponse createGroup(@Valid @RequestBody ApprovalGroupRequest request) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(DataPolicyContextHolder.get());
        return ApprovalGroupResponse.apply(approvalGroupService.createApprovalGroup(request, context, true), masker);
    }

    @GetMapping("/{id}")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    @Operation(summary = "승인그룹 상세 조회")
    public ApprovalGroupResponse getGroup(@PathVariable UUID id) {
        var masker = MaskingFunctions.masker(DataPolicyContextHolder.get());
        return ApprovalGroupResponse.apply(approvalGroupService.getApprovalGroup(id), masker);
    }

    @PutMapping("/{id}")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    @Operation(summary = "승인그룹 수정")
    public ApprovalGroupResponse updateGroup(@PathVariable UUID id,
                                             @Valid @RequestBody ApprovalGroupUpdateRequest request) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(DataPolicyContextHolder.get());
        return ApprovalGroupResponse.apply(approvalGroupService.updateApprovalGroup(id, request, context, true), masker);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    @Operation(summary = "승인그룹 삭제 (비활성화)")
    public void deleteGroup(@PathVariable UUID id) {
        AuthContext context = currentContext();
        approvalGroupService.deleteApprovalGroup(id, context, true);
    }

    @PostMapping("/{id}/activate")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    @Operation(summary = "승인그룹 활성화 (복원)")
    public ApprovalGroupResponse activateGroup(@PathVariable UUID id) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(DataPolicyContextHolder.get());
        return ApprovalGroupResponse.apply(approvalGroupService.activateApprovalGroup(id, context, true), masker);
    }

    @GetMapping
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    @Operation(summary = "승인그룹 목록 조회")
    public List<ApprovalGroupResponse> listGroups(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(DataPolicyContextHolder.get());
        return approvalGroupService.listApprovalGroups(keyword, activeOnly, context, true).stream()
                .map(g -> ApprovalGroupResponse.apply(g, masker))
                .toList();
    }

    @GetMapping("/exists")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    @Operation(summary = "승인그룹 코드 중복 검사")
    public GroupCodeExistsResponse checkGroupCodeExists(@RequestParam String groupCode) {
        return new GroupCodeExistsResponse(approvalGroupService.existsGroupCode(groupCode));
    }

    @PatchMapping("/priorities")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_AUDIT)
    @Operation(summary = "승인그룹 우선순위 일괄 업데이트")
    public List<ApprovalGroupResponse> updateGroupPriorities(@Valid @RequestBody ApprovalGroupPriorityRequest request) {
        AuthContext context = currentContext();
        var masker = MaskingFunctions.masker(DataPolicyContextHolder.get());
        return approvalGroupService.updateApprovalGroupPriorities(request, context, true).stream()
                .map(g -> ApprovalGroupResponse.apply(g, masker))
                .toList();
    }

    private AuthContext currentContext() {
        return AuthContextHolder.current().orElseThrow(() -> new PermissionDeniedException("인증 정보가 없습니다."));
    }
}
