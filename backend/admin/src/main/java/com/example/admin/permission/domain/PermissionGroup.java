package com.example.admin.permission.domain;

import com.example.common.jpa.PrimaryKeyEntity;
import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 권한 그룹 엔티티 (SCD Type 2 버전 관리).
 * <p>
 * 권한 그룹의 각 버전을 저장하여 시점 조회와 감사 추적을 지원합니다.
 * 비즈니스 데이터(name, description, assignments, approvalGroupCodes)를 포함합니다.
 * </p>
 */
@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "SE_BAD_FIELD"},
    justification = "Entity returns unmodifiable views; persistence proxies handle serialization")
@Entity
@Table(name = "permission_groups", indexes = {
    @Index(name = "idx_pg_root_version", columnList = "root_id, version"),
    @Index(name = "idx_pg_valid_range", columnList = "root_id, valid_from, valid_to")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PermissionGroup extends PrimaryKeyEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "root_id", nullable = false)
  private PermissionGroupRoot root;

  @Column(name = "version", nullable = false)
  private Integer version;

  @Column(name = "valid_from", nullable = false)
  private OffsetDateTime validFrom;

  /** NULL이면 현재 유효한 버전 */
  @Column(name = "valid_to")
  private OffsetDateTime validTo;

  // === 비즈니스 필드 (스냅샷) ===

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Column(name = "description", length = 1000)
  private String description;

  @Column(name = "active", nullable = false)
  private boolean active;

  /** 권한 할당 목록 (JSON 컬럼) */
  @Convert(converter = PermissionAssignmentListConverter.class)
  @Column(name = "assignments", columnDefinition = "jsonb")
  private List<PermissionAssignment> assignments = new ArrayList<>();

  /** 승인 그룹 코드 목록 (JSON 컬럼) */
  @Convert(converter = ApprovalGroupCodesConverter.class)
  @Column(name = "approval_group_codes", columnDefinition = "jsonb")
  private List<String> approvalGroupCodes = new ArrayList<>();

  // === 버전 상태 (Draft/Published) ===

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private VersionStatus status = VersionStatus.PUBLISHED;

  // === 감사 필드 ===

  @Enumerated(EnumType.STRING)
  @Column(name = "change_action", nullable = false, length = 20)
  private ChangeAction changeAction;

  @Column(name = "change_reason", length = 500)
  private String changeReason;

  @Column(name = "changed_by", nullable = false, length = 100)
  private String changedBy;

  @Column(name = "changed_by_name", length = 100)
  private String changedByName;

  @Column(name = "changed_at", nullable = false)
  private OffsetDateTime changedAt;

  /** ROLLBACK 시 롤백 대상 버전 번호 */
  @Column(name = "rollback_from_version")
  private Integer rollbackFromVersion;

  /** 버전 태그 (선택적) */
  @Column(name = "version_tag", length = 100)
  private String versionTag;

  private PermissionGroup(PermissionGroupRoot root,
                          Integer version,
                          String name,
                          String description,
                          boolean active,
                          List<PermissionAssignment> assignments,
                          List<String> approvalGroupCodes,
                          VersionStatus status,
                          ChangeAction changeAction,
                          String changeReason,
                          String changedBy,
                          String changedByName,
                          OffsetDateTime now,
                          Integer rollbackFromVersion) {
    this.root = root;
    this.version = version;
    this.validFrom = now;
    this.validTo = null;
    this.name = name;
    this.description = description;
    this.active = active;
    this.assignments = assignments != null ? new ArrayList<>(assignments) : new ArrayList<>();
    this.approvalGroupCodes = approvalGroupCodes != null ? new ArrayList<>(approvalGroupCodes) : new ArrayList<>();
    this.status = status;
    this.changeAction = changeAction;
    this.changeReason = changeReason;
    this.changedBy = changedBy;
    this.changedByName = changedByName;
    this.changedAt = now;
    this.rollbackFromVersion = rollbackFromVersion;
  }

  /**
   * 새 버전 생성 (일반적인 생성/수정/삭제 등).
   */
  public static PermissionGroup create(PermissionGroupRoot root,
                                        Integer version,
                                        String name,
                                        String description,
                                        boolean active,
                                        List<PermissionAssignment> assignments,
                                        List<String> approvalGroupCodes,
                                        ChangeAction changeAction,
                                        String changeReason,
                                        String changedBy,
                                        String changedByName,
                                        OffsetDateTime now) {
    return new PermissionGroup(
        root, version, name, description, active, assignments, approvalGroupCodes,
        VersionStatus.PUBLISHED, changeAction, changeReason,
        changedBy, changedByName, now, null);
  }

  /**
   * 초안(Draft) 버전 생성.
   */
  public static PermissionGroup createDraft(PermissionGroupRoot root,
                                             Integer version,
                                             String name,
                                             String description,
                                             boolean active,
                                             List<PermissionAssignment> assignments,
                                             List<String> approvalGroupCodes,
                                             String changeReason,
                                             String changedBy,
                                             String changedByName,
                                             OffsetDateTime now) {
    return new PermissionGroup(
        root, version, name, description, active, assignments, approvalGroupCodes,
        VersionStatus.DRAFT, ChangeAction.DRAFT, changeReason,
        changedBy, changedByName, now, null);
  }

  /**
   * 롤백 시 버전 생성.
   */
  public static PermissionGroup createFromRollback(PermissionGroupRoot root,
                                                    Integer version,
                                                    String name,
                                                    String description,
                                                    boolean active,
                                                    List<PermissionAssignment> assignments,
                                                    List<String> approvalGroupCodes,
                                                    String changeReason,
                                                    String changedBy,
                                                    String changedByName,
                                                    OffsetDateTime now,
                                                    Integer rollbackFromVersion) {
    return new PermissionGroup(
        root, version, name, description, active, assignments, approvalGroupCodes,
        VersionStatus.PUBLISHED, ChangeAction.ROLLBACK, changeReason,
        changedBy, changedByName, now, rollbackFromVersion);
  }

  /**
   * 현재 유효한 버전인지 확인.
   */
  public boolean isCurrent() {
    return this.validTo == null && this.status == VersionStatus.PUBLISHED;
  }

  /**
   * 초안 상태인지 확인.
   */
  public boolean isDraft() {
    return this.status == VersionStatus.DRAFT;
  }

  /**
   * 버전 종료 (새 버전이 생기면 호출).
   */
  public void close(OffsetDateTime closedAt) {
    this.validTo = closedAt;
    if (this.status == VersionStatus.PUBLISHED) {
      this.status = VersionStatus.HISTORICAL;
    }
  }

  /**
   * 초안을 게시 상태로 전환.
   */
  public void publish(OffsetDateTime publishedAt) {
    if (this.status != VersionStatus.DRAFT) {
      throw new IllegalStateException("초안 상태에서만 게시할 수 있습니다. 현재 상태: " + this.status);
    }
    this.status = VersionStatus.PUBLISHED;
    this.changeAction = ChangeAction.PUBLISH;
    this.validFrom = publishedAt;
    this.changedAt = publishedAt;
  }

  /**
   * 초안 내용 업데이트.
   */
  public void updateDraft(String name,
                          String description,
                          boolean active,
                          List<PermissionAssignment> assignments,
                          List<String> approvalGroupCodes,
                          String changeReason,
                          OffsetDateTime now) {
    if (this.status != VersionStatus.DRAFT) {
      throw new IllegalStateException("초안 상태에서만 수정할 수 있습니다. 현재 상태: " + this.status);
    }
    this.name = name;
    this.description = description;
    this.active = active;
    this.assignments = assignments != null ? new ArrayList<>(assignments) : new ArrayList<>();
    this.approvalGroupCodes = approvalGroupCodes != null ? new ArrayList<>(approvalGroupCodes) : new ArrayList<>();
    this.changeReason = changeReason;
    this.changedAt = now;
  }

  /**
   * 버전 태그 설정.
   */
  public void setVersionTag(String tag) {
    this.versionTag = tag;
  }

  // === 편의 메서드 ===

  /**
   * 권한 그룹 코드를 반환합니다.
   */
  public String getCode() {
    return root != null ? root.getGroupCode() : null;
  }

  /**
   * 권한 할당 목록을 반환합니다 (불변).
   */
  public List<PermissionAssignment> getAssignments() {
    return Collections.unmodifiableList(assignments);
  }

  /**
   * 특정 feature/action에 대한 권한 할당을 조회합니다.
   */
  public Optional<PermissionAssignment> assignmentFor(FeatureCode feature, ActionCode action) {
    return assignments.stream()
        .filter(a -> a.getFeature() == feature && a.getAction() == action)
        .findFirst();
  }

  /**
   * 승인 그룹 코드 목록을 반환합니다 (불변).
   */
  public List<String> getApprovalGroupCodes() {
    return Collections.unmodifiableList(approvalGroupCodes);
  }
}
