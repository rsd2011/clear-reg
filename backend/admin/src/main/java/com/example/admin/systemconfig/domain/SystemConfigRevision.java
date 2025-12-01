package com.example.admin.systemconfig.domain;

import com.example.common.jpa.PrimaryKeyEntity;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 시스템 설정 리비전 엔티티 (SCD Type 2 버전 관리).
 * <p>
 * 시스템 설정의 각 버전을 저장하여 시점 조회와 감사 추적을 지원합니다.
 * YAML 형식의 계층적 설정 데이터를 저장합니다.
 * </p>
 */
@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Entity returns unmodifiable views; persistence proxies handle serialization")
@Entity
@Table(name = "system_config_revisions", indexes = {
    @Index(name = "idx_screv_root_version", columnList = "root_id, version"),
    @Index(name = "idx_screv_valid_range", columnList = "root_id, valid_from, valid_to"),
    @Index(name = "idx_screv_status", columnList = "root_id, status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SystemConfigRevision extends PrimaryKeyEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "root_id", nullable = false)
  private SystemConfigRoot root;

  @Column(name = "version", nullable = false)
  private Integer version;

  @Column(name = "valid_from", nullable = false)
  private OffsetDateTime validFrom;

  /** NULL이면 현재 유효한 버전 */
  @Column(name = "valid_to")
  private OffsetDateTime validTo;

  // === 비즈니스 필드 ===

  /** YAML 형식의 설정 내용 */
  @Column(name = "yaml_content", columnDefinition = "text", nullable = false)
  private String yamlContent;

  /** 활성화 상태 */
  @Column(name = "active", nullable = false)
  private boolean active;

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

  private SystemConfigRevision(SystemConfigRoot root,
      Integer version,
      String yamlContent,
      boolean active,
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
    this.yamlContent = yamlContent;
    this.active = active;
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
  public static SystemConfigRevision create(SystemConfigRoot root,
      Integer version,
      String yamlContent,
      boolean active,
      ChangeAction changeAction,
      String changeReason,
      String changedBy,
      String changedByName,
      OffsetDateTime now) {
    return new SystemConfigRevision(
        root, version, yamlContent, active,
        VersionStatus.PUBLISHED, changeAction, changeReason,
        changedBy, changedByName, now, null);
  }

  /**
   * 초안(Draft) 버전 생성.
   */
  public static SystemConfigRevision createDraft(SystemConfigRoot root,
      Integer version,
      String yamlContent,
      boolean active,
      String changeReason,
      String changedBy,
      String changedByName,
      OffsetDateTime now) {
    return new SystemConfigRevision(
        root, version, yamlContent, active,
        VersionStatus.DRAFT, ChangeAction.DRAFT, changeReason,
        changedBy, changedByName, now, null);
  }

  /**
   * 롤백 시 버전 생성.
   */
  public static SystemConfigRevision createFromRollback(SystemConfigRoot root,
      Integer version,
      String yamlContent,
      boolean active,
      String changeReason,
      String changedBy,
      String changedByName,
      OffsetDateTime now,
      Integer rollbackFromVersion) {
    return new SystemConfigRevision(
        root, version, yamlContent, active,
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
  public void updateDraft(String yamlContent,
      boolean active,
      String changeReason,
      OffsetDateTime now) {
    if (this.status != VersionStatus.DRAFT) {
      throw new IllegalStateException("초안 상태에서만 수정할 수 있습니다. 현재 상태: " + this.status);
    }
    this.yamlContent = yamlContent;
    this.active = active;
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
   * 설정 코드를 반환합니다.
   */
  public String getConfigCode() {
    return root != null ? root.getConfigCode() : null;
  }
}
