package com.example.admin.systemconfig.domain;

import com.example.common.jpa.PrimaryKeyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 시스템 설정 루트 엔티티.
 * <p>
 * 버전 컨테이너 역할을 하며, 실제 설정 데이터는 {@link SystemConfigRevision}에 저장됩니다.
 * SCD Type 2 패턴을 적용하여 시스템 설정의 변경 이력을 관리합니다.
 * </p>
 * <p>
 * 설정 코드 예시:
 * <ul>
 *   <li>auth.settings - 인증 관련 설정</li>
 *   <li>file.settings - 파일 업로드 관련 설정</li>
 *   <li>audit.settings - 감사 로그 관련 설정</li>
 * </ul>
 * </p>
 */
@Entity
@Table(name = "system_config_roots", indexes = {
    @Index(name = "idx_scr_config_code", columnList = "config_code", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SystemConfigRoot extends PrimaryKeyEntity {

  /** 설정 코드 (예: auth.settings, file.settings, audit.settings) */
  @Column(name = "config_code", nullable = false, length = 100, unique = true)
  private String configCode;

  /** 설정명 */
  @Column(name = "name", nullable = false, length = 255)
  private String name;

  /** 설정 설명 */
  @Column(name = "description", length = 1000)
  private String description;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  // === 버전 링크 (SCD Type 2) ===

  /** 현재 활성 버전 */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "current_version_id")
  private SystemConfigRevision currentVersion;

  /** 이전 활성 버전 (롤백 참조용) */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "previous_version_id")
  private SystemConfigRevision previousVersion;

  /** 다음 예약 버전 (Draft 또는 결재 대기용) */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "next_version_id")
  private SystemConfigRevision nextVersion;

  private SystemConfigRoot(String configCode, String name, String description, OffsetDateTime now) {
    this.configCode = configCode;
    this.name = name;
    this.description = description;
    this.createdAt = now;
    this.updatedAt = now;
  }

  /**
   * 새 시스템 설정 루트를 생성합니다.
   *
   * @param configCode 설정 코드 (예: auth.settings)
   * @param name 설정명
   * @param description 설정 설명
   * @param now 생성 시점
   * @return 새 SystemConfigRoot 인스턴스
   */
  public static SystemConfigRoot create(String configCode, String name, String description,
      OffsetDateTime now) {
    return new SystemConfigRoot(configCode, name, description, now);
  }

  /**
   * 업데이트 시간을 갱신합니다.
   */
  public void touch(OffsetDateTime now) {
    this.updatedAt = now;
  }

  /**
   * 설정 기본 정보를 수정합니다.
   */
  public void updateInfo(String name, String description, OffsetDateTime now) {
    this.name = name;
    this.description = description;
    this.updatedAt = now;
  }

  // === 버전 관리 메서드 ===

  /**
   * 새 버전을 활성화합니다.
   * 현재 버전을 이전 버전으로 이동하고, 새 버전을 현재 버전으로 설정합니다.
   *
   * @param newVersion 새로 활성화할 버전
   * @param now 활성화 시점
   */
  public void activateNewVersion(SystemConfigRevision newVersion, OffsetDateTime now) {
    this.previousVersion = this.currentVersion;
    this.currentVersion = newVersion;
    this.nextVersion = null;
    this.updatedAt = now;
  }

  /**
   * 초안 버전을 설정합니다.
   *
   * @param draftVersion 초안 버전
   */
  public void setDraftVersion(SystemConfigRevision draftVersion) {
    this.nextVersion = draftVersion;
  }

  /**
   * 초안을 게시합니다.
   * 현재 버전을 종료하고 초안을 현재 버전으로 전환합니다.
   *
   * @param now 게시 시점
   */
  public void publishDraft(OffsetDateTime now) {
    if (this.nextVersion == null || !this.nextVersion.isDraft()) {
      throw new IllegalStateException("게시할 초안 버전이 없습니다.");
    }

    // 현재 버전 종료
    if (this.currentVersion != null) {
      this.currentVersion.close(now);
    }

    // 초안 게시
    this.nextVersion.publish(now);

    // 버전 링크 업데이트
    this.previousVersion = this.currentVersion;
    this.currentVersion = this.nextVersion;
    this.nextVersion = null;
    this.updatedAt = now;
  }

  /**
   * 초안을 삭제합니다.
   */
  public void discardDraft() {
    this.nextVersion = null;
  }

  /**
   * 이전 버전으로 롤백할 수 있는지 확인합니다.
   */
  public boolean canRollback() {
    return this.previousVersion != null;
  }

  /**
   * 현재 버전 번호를 반환합니다.
   */
  public Integer getCurrentVersionNumber() {
    return currentVersion != null ? currentVersion.getVersion() : null;
  }

  /**
   * 초안이 있는지 확인합니다.
   */
  public boolean hasDraft() {
    return nextVersion != null && nextVersion.isDraft();
  }

  // === 편의 메서드 (현재 버전에서 조회) ===

  /**
   * 현재 버전의 YAML 설정 내용을 반환합니다.
   */
  public String getYamlContent() {
    return currentVersion != null ? currentVersion.getYamlContent() : null;
  }

  /**
   * 현재 버전의 활성 상태를 반환합니다.
   */
  public boolean isActive() {
    return currentVersion != null && currentVersion.isActive();
  }
}
