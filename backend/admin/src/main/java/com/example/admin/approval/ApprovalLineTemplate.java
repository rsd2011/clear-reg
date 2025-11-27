package com.example.admin.approval;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.example.admin.approval.version.ApprovalLineTemplateVersion;
import com.example.common.jpa.PrimaryKeyEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "approval_line_templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalLineTemplate extends PrimaryKeyEntity {

    @Column(name = "template_code", nullable = false, length = 100, unique = true)
    private String templateCode;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // === 버전 링크 (SCD Type 2) ===

    /** 현재 활성 버전 */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_version_id")
    private ApprovalLineTemplateVersion currentVersion;

    /** 이전 활성 버전 (롤백 참조용) */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_version_id")
    private ApprovalLineTemplateVersion previousVersion;

    /** 다음 예약 버전 (Draft 또는 결재 대기용) */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_version_id")
    private ApprovalLineTemplateVersion nextVersion;

    // === 기존 Steps (하위 호환성 유지, 추후 제거 예정) ===

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("stepOrder ASC")
    private final List<ApprovalTemplateStep> steps = new ArrayList<>();

    private ApprovalLineTemplate(String templateCode,
                                 String name,
                                 Integer displayOrder,
                                 String description,
                                 OffsetDateTime now) {
        this.templateCode = templateCode;
        this.name = name;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.description = description;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static ApprovalLineTemplate create(String name, Integer displayOrder, String description, OffsetDateTime now) {
        return new ApprovalLineTemplate(UUID.randomUUID().toString(), name, displayOrder, description, now);
    }

    public void rename(String name, Integer displayOrder, String description, boolean active, OffsetDateTime now) {
        this.name = name;
        this.displayOrder = displayOrder != null ? displayOrder : this.displayOrder;
        this.description = description;
        this.active = active;
        this.updatedAt = now;
    }

    public void replaceSteps(List<ApprovalTemplateStep> newSteps) {
        this.steps.clear();
        this.steps.addAll(newSteps);
        this.steps.sort(Comparator.comparingInt(ApprovalTemplateStep::getStepOrder));
    }

    public void addStep(int stepOrder, ApprovalGroup approvalGroup) {
        ApprovalTemplateStep step = new ApprovalTemplateStep(this, stepOrder, approvalGroup);
        this.steps.add(step);
        this.steps.sort(Comparator.comparingInt(ApprovalTemplateStep::getStepOrder));
    }

    // === 버전 관리 메서드 ===

    /**
     * 새 버전을 활성화합니다.
     * 현재 버전을 이전 버전으로 이동하고, 새 버전을 현재 버전으로 설정합니다.
     *
     * @param newVersion 새로 활성화할 버전
     * @param now        활성화 시점
     */
    public void activateNewVersion(ApprovalLineTemplateVersion newVersion, OffsetDateTime now) {
        this.previousVersion = this.currentVersion;
        this.currentVersion = newVersion;
        this.nextVersion = null;
        syncFromVersion(newVersion, now);
    }

    /**
     * 초안 버전을 설정합니다.
     *
     * @param draftVersion 초안 버전
     */
    public void setDraftVersion(ApprovalLineTemplateVersion draftVersion) {
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

        syncFromVersion(this.currentVersion, now);
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
     * 버전에서 메인 테이블 필드를 동기화합니다.
     */
    private void syncFromVersion(ApprovalLineTemplateVersion version, OffsetDateTime now) {
        this.name = version.getName();
        this.displayOrder = version.getDisplayOrder();
        this.description = version.getDescription();
        this.active = version.isActive();
        this.updatedAt = now;
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
}
