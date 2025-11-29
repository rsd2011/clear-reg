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
import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.admin.permission.exception.PermissionDeniedException;
import com.example.admin.rowaccesspolicy.dto.RowAccessPolicyDraftRequest;
import com.example.admin.rowaccesspolicy.dto.RowAccessPolicyHistoryResponse;
import com.example.admin.rowaccesspolicy.dto.RowAccessPolicyRootRequest;
import com.example.admin.rowaccesspolicy.dto.RowAccessPolicyRootResponse;
import com.example.admin.rowaccesspolicy.service.RowAccessPolicyRootService;
import com.example.admin.rowaccesspolicy.service.RowAccessPolicyVersioningService;
import com.example.common.security.RowScope;

/**
 * 행 접근 정책 관리 API.
 */
@RestController
@Validated
@RequestMapping("/api/row-access-policies")
@Tag(name = "Row Access Policy Admin", description = "행 접근 정책 관리 API")
public class RowAccessPolicyRootController {

    private final RowAccessPolicyRootService policyService;
    private final RowAccessPolicyVersioningService versionService;

    public RowAccessPolicyRootController(RowAccessPolicyRootService policyService,
                                          RowAccessPolicyVersioningService versionService) {
        this.policyService = policyService;
        this.versionService = versionService;
    }

    // ==========================================================================
    // CRUD API
    // ==========================================================================

    @GetMapping
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.READ)
    @Operation(summary = "행 접근 정책 목록 조회")
    public List<RowAccessPolicyRootResponse> listPolicies(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return policyService.list(keyword, activeOnly);
    }

    @GetMapping("/active")
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.READ)
    @Operation(summary = "활성 행 접근 정책 목록 조회 (우선순위순)")
    public List<RowAccessPolicyRootResponse> listActivePolicies() {
        return policyService.listActive();
    }

    @GetMapping("/by-feature/{featureCode}")
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.READ)
    @Operation(summary = "특정 FeatureCode의 행 접근 정책 목록 조회")
    public List<RowAccessPolicyRootResponse> listByFeatureCode(@PathVariable FeatureCode featureCode) {
        return policyService.listByFeatureCode(featureCode);
    }

    @GetMapping("/by-scope/{rowScope}")
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.READ)
    @Operation(summary = "특정 RowScope의 행 접근 정책 목록 조회")
    public List<RowAccessPolicyRootResponse> listByRowScope(@PathVariable RowScope rowScope) {
        return policyService.listByRowScope(rowScope);
    }

    @GetMapping("/{id}")
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.READ)
    @Operation(summary = "행 접근 정책 단일 조회")
    public RowAccessPolicyRootResponse getPolicy(@PathVariable UUID id) {
        return policyService.getById(id);
    }

    @GetMapping("/by-code/{policyCode}")
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.READ)
    @Operation(summary = "정책 코드로 행 접근 정책 조회")
    public RowAccessPolicyRootResponse getByPolicyCode(@PathVariable String policyCode) {
        return policyService.getByPolicyCode(policyCode);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.CREATE)
    @Operation(summary = "행 접근 정책 생성")
    public RowAccessPolicyRootResponse createPolicy(@Valid @RequestBody RowAccessPolicyRootRequest request) {
        AuthContext context = currentContext();
        return policyService.create(request, context);
    }

    @PutMapping("/{id}")
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.UPDATE)
    @Operation(summary = "행 접근 정책 수정")
    public RowAccessPolicyRootResponse updatePolicy(@PathVariable UUID id,
                                                     @Valid @RequestBody RowAccessPolicyRootRequest request) {
        AuthContext context = currentContext();
        return policyService.update(id, request, context);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.DELETE)
    @Operation(summary = "행 접근 정책 삭제 (비활성화)")
    public void deletePolicy(@PathVariable UUID id) {
        AuthContext context = currentContext();
        policyService.delete(id, context);
    }

    @PostMapping("/{id}/activate")
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.UPDATE)
    @Operation(summary = "행 접근 정책 활성화 (복원)")
    public RowAccessPolicyRootResponse activatePolicy(@PathVariable UUID id) {
        AuthContext context = currentContext();
        return policyService.activate(id, context);
    }

    @GetMapping("/{id}/history")
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.READ)
    @Operation(summary = "행 접근 정책 변경 이력 조회")
    public List<RowAccessPolicyHistoryResponse> getPolicyHistory(@PathVariable UUID id) {
        return policyService.getHistory(id);
    }

    // ==========================================================================
    // SCD Type 2 버전 관리 API
    // ==========================================================================

    @GetMapping("/{id}/versions")
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.READ)
    @Operation(summary = "버전 이력 조회 (SCD Type 2)")
    public List<RowAccessPolicyHistoryResponse> getVersionHistory(@PathVariable UUID id) {
        return versionService.getVersionHistory(id);
    }

    @GetMapping("/{id}/versions/{version}")
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.READ)
    @Operation(summary = "특정 버전 상세 조회")
    public RowAccessPolicyHistoryResponse getVersion(@PathVariable UUID id, @PathVariable Integer version) {
        return versionService.getVersion(id, version);
    }

    @GetMapping("/{id}/versions/as-of")
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.READ)
    @Operation(summary = "특정 시점 버전 조회 (Point-in-Time Query)")
    public RowAccessPolicyHistoryResponse getVersionAsOf(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) OffsetDateTime asOf) {
        return versionService.getVersionAsOf(id, asOf);
    }

    // ==========================================================================
    // 버전 롤백 (이력화면 시나리오)
    // ==========================================================================

    @PostMapping("/{id}/versions/rollback")
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.UPDATE)
    @Operation(summary = "버전 롤백", description = "이력화면에서 특정 버전을 선택하여 해당 시점의 상태로 복원합니다.")
    public RowAccessPolicyHistoryResponse rollbackToVersion(
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
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.READ)
    @Operation(summary = "초안 조회")
    public RowAccessPolicyHistoryResponse getDraft(@PathVariable UUID id) {
        return versionService.getDraft(id);
    }

    @GetMapping("/{id}/draft/exists")
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.READ)
    @Operation(summary = "초안 존재 여부 확인")
    public boolean hasDraft(@PathVariable UUID id) {
        return versionService.hasDraft(id);
    }

    @PostMapping("/{id}/draft")
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.UPDATE)
    @Operation(summary = "초안 저장", description = "정책을 수정할 때 바로 적용하지 않고 초안으로 저장합니다.")
    public RowAccessPolicyHistoryResponse saveDraft(
            @PathVariable UUID id,
            @Valid @RequestBody RowAccessPolicyDraftRequest request) {
        AuthContext context = currentContext();
        return versionService.saveDraft(id, request, context);
    }

    @PostMapping("/{id}/draft/publish")
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.UPDATE)
    @Operation(summary = "초안 게시", description = "초안을 현재 활성 버전으로 전환합니다.")
    public RowAccessPolicyHistoryResponse publishDraft(@PathVariable UUID id) {
        AuthContext context = currentContext();
        return versionService.publishDraft(id, context);
    }

    @DeleteMapping("/{id}/draft")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.UPDATE)
    @Operation(summary = "초안 삭제", description = "저장된 초안을 삭제합니다.")
    public void discardDraft(@PathVariable UUID id) {
        versionService.discardDraft(id);
    }

    // ==========================================================================
    // 초안이 있는 정책 목록
    // ==========================================================================

    @GetMapping("/with-draft")
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.READ)
    @Operation(summary = "초안이 있는 정책 목록 조회")
    public List<RowAccessPolicyRootResponse> listWithDraft() {
        return policyService.listWithDraft();
    }

    private AuthContext currentContext() {
        return AuthContextHolder.current().orElseThrow(() -> new PermissionDeniedException("인증 정보가 없습니다."));
    }
}
