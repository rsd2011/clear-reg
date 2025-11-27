package com.example.admin.approval;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.example.common.jpa.PrimaryKeyEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
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
}
