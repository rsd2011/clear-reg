package com.example.admin.systemconfig.web;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.admin.systemconfig.dto.SystemConfigCompareResponse;
import com.example.admin.systemconfig.dto.SystemConfigDraftRequest;
import com.example.admin.systemconfig.dto.SystemConfigRevisionResponse;
import com.example.admin.systemconfig.dto.SystemConfigRootRequest;
import com.example.admin.systemconfig.dto.SystemConfigRootResponse;
import com.example.admin.systemconfig.service.SystemConfigVersioningService;

import jakarta.validation.Valid;

/**
 * 시스템 설정 관리 REST API 컨트롤러.
 * <p>
 * 활성화 조건: SystemConfigVersioningService 빈이 존재해야 함
 * </p>
 */
@RestController
@RequestMapping("/api/admin/system-configs")
@ConditionalOnBean(SystemConfigVersioningService.class)
public class SystemConfigController {

  private final SystemConfigVersioningService versioningService;

  public SystemConfigController(SystemConfigVersioningService versioningService) {
    this.versioningService = versioningService;
  }

  // ==========================================================================
  // 설정 루트 관리
  // ==========================================================================

  /**
   * 모든 시스템 설정 목록 조회.
   */
  @GetMapping
  public ResponseEntity<List<SystemConfigRootResponse>> getAllConfigs() {
    return ResponseEntity.ok(versioningService.getAllConfigs());
  }

  /**
   * 설정 코드로 시스템 설정 조회.
   */
  @GetMapping("/by-code/{configCode}")
  public ResponseEntity<SystemConfigRootResponse> getConfigByCode(@PathVariable String configCode) {
    return ResponseEntity.ok(versioningService.getConfigByCode(configCode));
  }

  /**
   * 새 시스템 설정 생성.
   */
  @PostMapping
  public ResponseEntity<SystemConfigRootResponse> createConfig(
      @Valid @RequestBody SystemConfigRootRequest request) {
    AuthContext context = getAuthContext();
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(versioningService.createConfig(request, context));
  }

  /**
   * 시스템 설정 수정 (새 버전 생성).
   */
  @PutMapping("/{configId}")
  public ResponseEntity<SystemConfigRevisionResponse> updateConfig(
      @PathVariable UUID configId,
      @Valid @RequestBody SystemConfigDraftRequest request) {
    AuthContext context = getAuthContext();
    return ResponseEntity.ok(versioningService.updateConfig(configId, request, context));
  }

  /**
   * 시스템 설정 삭제 (비활성화).
   */
  @DeleteMapping("/{configId}")
  public ResponseEntity<SystemConfigRevisionResponse> deleteConfig(@PathVariable UUID configId) {
    AuthContext context = getAuthContext();
    return ResponseEntity.ok(versioningService.deleteConfig(configId, context));
  }

  // ==========================================================================
  // 버전 이력 조회
  // ==========================================================================

  /**
   * 버전 이력 목록 조회.
   */
  @GetMapping("/{configId}/versions")
  public ResponseEntity<List<SystemConfigRevisionResponse>> getVersionHistory(
      @PathVariable UUID configId) {
    return ResponseEntity.ok(versioningService.getVersionHistory(configId));
  }

  /**
   * 특정 버전 상세 조회.
   */
  @GetMapping("/{configId}/versions/{versionNumber}")
  public ResponseEntity<SystemConfigRevisionResponse> getVersion(
      @PathVariable UUID configId,
      @PathVariable Integer versionNumber) {
    return ResponseEntity.ok(versioningService.getVersion(configId, versionNumber));
  }

  /**
   * 특정 시점의 버전 조회 (Point-in-Time Query).
   */
  @GetMapping("/{configId}/versions/as-of")
  public ResponseEntity<SystemConfigRevisionResponse> getVersionAsOf(
      @PathVariable UUID configId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime asOf) {
    return ResponseEntity.ok(versioningService.getVersionAsOf(configId, asOf));
  }

  /**
   * 두 버전 비교.
   */
  @GetMapping("/{configId}/versions/compare")
  public ResponseEntity<SystemConfigCompareResponse> compareVersions(
      @PathVariable UUID configId,
      @RequestParam Integer version1,
      @RequestParam Integer version2) {
    return ResponseEntity.ok(versioningService.compareVersions(configId, version1, version2));
  }

  // ==========================================================================
  // 버전 롤백
  // ==========================================================================

  /**
   * 특정 버전으로 롤백.
   */
  @PostMapping("/{configId}/versions/{targetVersion}/rollback")
  public ResponseEntity<SystemConfigRevisionResponse> rollbackToVersion(
      @PathVariable UUID configId,
      @PathVariable Integer targetVersion,
      @RequestParam(required = false) String changeReason) {
    AuthContext context = getAuthContext();
    return ResponseEntity.ok(
        versioningService.rollbackToVersion(configId, targetVersion, changeReason, context));
  }

  // ==========================================================================
  // Draft/Publish 워크플로우
  // ==========================================================================

  /**
   * 초안 조회.
   */
  @GetMapping("/{configId}/draft")
  public ResponseEntity<SystemConfigRevisionResponse> getDraft(@PathVariable UUID configId) {
    return ResponseEntity.ok(versioningService.getDraft(configId));
  }

  /**
   * 초안 존재 여부 확인.
   */
  @GetMapping("/{configId}/draft/exists")
  public ResponseEntity<Boolean> hasDraft(@PathVariable UUID configId) {
    return ResponseEntity.ok(versioningService.hasDraft(configId));
  }

  /**
   * 초안 생성 또는 수정.
   */
  @PostMapping("/{configId}/draft")
  public ResponseEntity<SystemConfigRevisionResponse> saveDraft(
      @PathVariable UUID configId,
      @Valid @RequestBody SystemConfigDraftRequest request) {
    AuthContext context = getAuthContext();
    return ResponseEntity.ok(versioningService.saveDraft(configId, request, context));
  }

  /**
   * 초안 게시 (적용).
   */
  @PostMapping("/{configId}/draft/publish")
  public ResponseEntity<SystemConfigRevisionResponse> publishDraft(@PathVariable UUID configId) {
    AuthContext context = getAuthContext();
    return ResponseEntity.ok(versioningService.publishDraft(configId, context));
  }

  /**
   * 초안 삭제 (취소).
   */
  @DeleteMapping("/{configId}/draft")
  public ResponseEntity<Void> discardDraft(@PathVariable UUID configId) {
    versioningService.discardDraft(configId);
    return ResponseEntity.noContent().build();
  }

  // ==========================================================================
  // 헬퍼 메서드
  // ==========================================================================

  private AuthContext getAuthContext() {
    return AuthContextHolder.current()
        .orElseThrow(() -> new IllegalStateException("인증 컨텍스트가 없습니다."));
  }
}
