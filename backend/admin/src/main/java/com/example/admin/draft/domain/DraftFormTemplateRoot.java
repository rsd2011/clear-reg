package com.example.admin.draft.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.common.jpa.PrimaryKeyEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기안 양식 템플릿 루트 엔티티.
 * <p>
 * 버전 컨테이너 역할을 하며, 실제 비즈니스 데이터는 {@link DraftFormTemplate}에 저장됩니다.
 * </p>
 */
@Entity
@Table(name = "draft_form_template_roots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DraftFormTemplateRoot extends PrimaryKeyEntity {

    @Column(name = "template_code", nullable = false, length = 100, unique = true)
    private String templateCode;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // === 버전 링크 (SCD Type 2) ===

    /** 현재 활성 버전 */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_version_id")
    private DraftFormTemplate currentVersion;

    /** 이전 활성 버전 (롤백 참조용) */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_version_id")
    private DraftFormTemplate previousVersion;

    /** 다음 예약 버전 (Draft 또는 결재 대기용) */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_version_id")
    private DraftFormTemplate nextVersion;

    private DraftFormTemplateRoot(String templateCode, OffsetDateTime now) {
        this.templateCode = templateCode;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static DraftFormTemplateRoot create(OffsetDateTime now) {
        return new DraftFormTemplateRoot(UUID.randomUUID().toString(), now);
    }

    public static DraftFormTemplateRoot createWithCode(String templateCode, OffsetDateTime now) {
        return new DraftFormTemplateRoot(templateCode, now);
    }

    /**
     * 업데이트 시간을 갱신합니다.
     */
    public void touch(OffsetDateTime now) {
        this.updatedAt = now;
    }

    // === 버전 관리 메서드 ===

    /**
     * 새 버전을 활성화합니다.
     * 현재 버전을 이전 버전으로 이동하고, 새 버전을 현재 버전으로 설정합니다.
     *
     * @param newVersion 새로 활성화할 버전
     * @param now        활성화 시점
     */
    public void activateNewVersion(DraftFormTemplate newVersion, OffsetDateTime now) {
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
    public void setDraftVersion(DraftFormTemplate draftVersion) {
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
     * 현재 버전의 이름을 반환합니다.
     */
    public String getName() {
        return currentVersion != null ? currentVersion.getName() : null;
    }

    /**
     * 현재 버전의 업무유형을 반환합니다.
     */
    public com.example.common.orggroup.WorkType getWorkType() {
        return currentVersion != null ? currentVersion.getWorkType() : null;
    }

    /**
     * 현재 버전의 활성 상태를 반환합니다.
     */
    public boolean isActive() {
        return currentVersion != null && currentVersion.isActive();
    }
}
