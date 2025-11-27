package com.example.admin.approval;

import com.example.common.jpa.PrimaryKeyEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "approval_template_steps")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalTemplateStep extends PrimaryKeyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_template_id", nullable = false)
    private ApprovalLineTemplate template;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_group_id", nullable = false)
    private ApprovalGroup approvalGroup;

    public ApprovalTemplateStep(ApprovalLineTemplate template,
                                int stepOrder,
                                ApprovalGroup approvalGroup) {
        this.template = template;
        this.stepOrder = stepOrder;
        this.approvalGroup = approvalGroup;
    }
}
