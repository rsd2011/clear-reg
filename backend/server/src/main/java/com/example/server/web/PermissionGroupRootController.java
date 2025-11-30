package com.example.server.web;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.example.admin.permission.annotation.RequirePermission;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.admin.permission.dto.PermissionGroupCompareResponse;
import com.example.admin.permission.dto.PermissionGroupDraftRequest;
import com.example.admin.permission.dto.PermissionGroupHistoryResponse;
import com.example.admin.permission.dto.PermissionGroupRootRequest;
import com.example.admin.permission.dto.PermissionGroupRootResponse;
import com.example.admin.permission.exception.PermissionDeniedException;
import com.example.admin.permission.service.PermissionGroupRootService;
import com.example.admin.permission.service.PermissionGroupVersioningService;
import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;

/**
 * 권한 그룹 관리 API.
 */
@RestController
@Validated
@RequestMapping("/api/permission-groups")
@Tag(name = "Permission Group Admin", description = "권한 그룹 관리 API")
public class PermissionGroupRootController {

    private final PermissionGroupRootService groupService;
    private final PermissionGroupVersioningService versionService;

    public PermissionGroupRootController(PermissionGroupRootService groupService,
                                          PermissionGroupVersioningService versionService) {
        this.groupService = groupService;
        this.versionService = versionService;
    }

    // ==========================================================================
    // 기본 조회 API
    // ==========================================================================

    @GetMapping
    @RequirePermission(feature = FeatureCode.RULE_MANAGE, action = ActionCode.READ)
    @Operation(summary = "권한 그룹 목록 조회")
    public List<PermissionGroupRootResponse> listGroups(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return groupService.list(keyword, activeOnly);
    }

    @GetMapping("/{id}")
    @RequirePermission(feature = FeatureCode.RULE_MANAGE, action = ActionCode.READ)
    @Operation(summary = "권한 그룹 단일 조회")
    public PermissionGroupRootResponse getGroup(@PathVariable UUID id) {
        return groupService.getById(id);
    }

    @GetMapping("/by-code/{groupCode}")
    @RequirePermission(feature = FeatureCode.RULE_MANAGE, action = ActionCode.READ)
    @Operation(summary = "그룹 코드로 권한 그룹 조회")
    public PermissionGroupRootResponse getByGroupCode(@PathVariable String groupCode) {
        return groupService.getByGroupCode(groupCode);
    }

    // ==========================================================================
    // 생성/삭제/활성화 API
    // ==========================================================================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(feature = FeatureCode.RULE_MANAGE, action = ActionCode.CREATE)
    @Operation(summary = "권한 그룹 생성")
    public PermissionGroupRootResponse createGroup(@Valid @RequestBody PermissionGroupRootRequest request) {
        AuthContext context = currentContext();
        if (request.groupCode() != null && !request.groupCode().isBlank()) {
            return groupService.createWithCode(request.groupCode(), request, context);
        }
        return groupService.create(request, context);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(feature = FeatureCode.RULE_MANAGE, action = ActionCode.DELETE)
    @Operation(summary = "권한 그룹 삭제 (비활성화)")
    public void deleteGroup(@PathVariable UUID id) {
        AuthContext context = currentContext();
        groupService.delete(id, context);
    }

    @PostMapping("/{id}/activate")
    @RequirePermission(feature = FeatureCode.RULE_MANAGE, action = ActionCode.UPDATE)
    @Operation(summary = "권한 그룹 활성화 (복원)")
    public PermissionGroupRootResponse activateGroup(@PathVariable UUID id) {
        AuthContext context = currentContext();
        return groupService.activate(id, context);
    }

    @GetMapping("/{id}/history")
    @RequirePermission(feature = FeatureCode.RULE_MANAGE, action = ActionCode.READ)
    @Operation(summary = "권한 그룹 변경 이력 조회")
    public List<PermissionGroupHistoryResponse> getGroupHistory(@PathVariable UUID id) {
        return groupService.getHistory(id);
    }

    // ==========================================================================
    // SCD Type 2 버전 관리 API
    // ==========================================================================

    @GetMapping("/{id}/versions")
    @RequirePermission(feature = FeatureCode.RULE_MANAGE, action = ActionCode.READ)
    @Operation(summary = "버전 이력 조회 (SCD Type 2)")
    public List<PermissionGroupHistoryResponse> getVersionHistory(@PathVariable UUID id) {
        return versionService.getVersionHistory(id);
    }

    @GetMapping("/{id}/versions/{version}")
    @RequirePermission(feature = FeatureCode.RULE_MANAGE, action = ActionCode.READ)
    @Operation(summary = "특정 버전 상세 조회")
    public PermissionGroupHistoryResponse getVersion(@PathVariable UUID id, @PathVariable Integer version) {
        return versionService.getVersion(id, version);
    }

    @GetMapping("/{id}/versions/as-of")
    @RequirePermission(feature = FeatureCode.RULE_MANAGE, action = ActionCode.READ)
    @Operation(summary = "특정 시점 버전 조회 (Point-in-Time Query)")
    public PermissionGroupHistoryResponse getVersionAsOf(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) OffsetDateTime asOf) {
        return versionService.getVersionAsOf(id, asOf);
    }

    @GetMapping("/{id}/versions/compare")
    @RequirePermission(feature = FeatureCode.RULE_MANAGE, action = ActionCode.READ)
    @Operation(summary = "두 버전 비교")
    public PermissionGroupCompareResponse compareVersions(
            @PathVariable UUID id,
            @RequestParam @NotNull(message = "버전1은 필수입니다") Integer v1,
            @RequestParam @NotNull(message = "버전2는 필수입니다") Integer v2) {
        return versionService.compareVersions(id, v1, v2);
    }

    // ==========================================================================
    // 버전 롤백 (이력화면 시나리오)
    // ==========================================================================

    @PostMapping("/{id}/versions/rollback")
    @RequirePermission(feature = FeatureCode.RULE_MANAGE, action = ActionCode.UPDATE)
    @Operation(summary = "버전 롤백", description = "이력화면에서 특정 버전을 선택하여 해당 시점의 상태로 복원합니다.")
    public PermissionGroupHistoryResponse rollbackToVersion(
            @PathVariable UUID id,
            @RequestParam @NotNull(message = "롤백할 버전 번호는 필수입니다")
            @Min(value = 1, message = "버전 번호는 1 이상이어야 합니다") Integer targetVersion,
            @RequestParam(required = false)
            @Size(max = 500, message = "변경 사유는 500자 이하여야 합니다") String changeReason) {
        AuthContext context = currentContext();
        return versionService.rollbackToVersion(id, targetVersion, changeReason, context);
    }

    // ==========================================================================
    // Draft/Published (목록화면 시나리오)
    // ==========================================================================

    @GetMapping("/{id}/draft")
    @RequirePermission(feature = FeatureCode.RULE_MANAGE, action = ActionCode.READ)
    @Operation(summary = "초안 조회")
    public PermissionGroupHistoryResponse getDraft(@PathVariable UUID id) {
        return versionService.getDraft(id);
    }

    @GetMapping("/{id}/draft/exists")
    @RequirePermission(feature = FeatureCode.RULE_MANAGE, action = ActionCode.READ)
    @Operation(summary = "초안 존재 여부 확인")
    public boolean hasDraft(@PathVariable UUID id) {
        return versionService.hasDraft(id);
    }

    @PostMapping("/{id}/draft")
    @RequirePermission(feature = FeatureCode.RULE_MANAGE, action = ActionCode.UPDATE)
    @Operation(summary = "초안 저장", description = "권한 그룹을 수정할 때 바로 적용하지 않고 초안으로 저장합니다.")
    public PermissionGroupHistoryResponse saveDraft(
            @PathVariable UUID id,
            @Valid @RequestBody PermissionGroupDraftRequest request) {
        AuthContext context = currentContext();
        return versionService.saveDraft(id, request, context);
    }

    @PostMapping("/{id}/draft/publish")
    @RequirePermission(feature = FeatureCode.RULE_MANAGE, action = ActionCode.UPDATE)
    @Operation(summary = "초안 게시", description = "초안을 현재 활성 버전으로 전환합니다.")
    public PermissionGroupHistoryResponse publishDraft(@PathVariable UUID id) {
        AuthContext context = currentContext();
        return versionService.publishDraft(id, context);
    }

    @DeleteMapping("/{id}/draft")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(feature = FeatureCode.RULE_MANAGE, action = ActionCode.UPDATE)
    @Operation(summary = "초안 삭제", description = "저장된 초안을 삭제합니다.")
    public void discardDraft(@PathVariable UUID id) {
        versionService.discardDraft(id);
    }

    // ==========================================================================
    // 초안이 있는 그룹 목록
    // ==========================================================================

    @GetMapping("/with-draft")
    @RequirePermission(feature = FeatureCode.RULE_MANAGE, action = ActionCode.READ)
    @Operation(summary = "초안이 있는 권한 그룹 목록 조회")
    public List<PermissionGroupRootResponse> listWithDraft() {
        return groupService.listWithDraft();
    }

    private AuthContext currentContext() {
        return AuthContextHolder.current().orElseThrow(() -> new PermissionDeniedException("인증 정보가 없습니다."));
    }
}
