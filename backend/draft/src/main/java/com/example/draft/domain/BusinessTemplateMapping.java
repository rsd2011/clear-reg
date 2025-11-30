package com.example.draft.domain;

import java.time.OffsetDateTime;

import com.example.admin.draft.domain.DraftFormTemplate;
import com.example.common.jpa.PrimaryKeyEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.example.admin.approval.domain.ApprovalTemplateRoot;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "business_template_mappings",
        indexes = {
                @Index(name = "idx_btm_business_org", columnList = "business_feature_code, organization_code", unique = true),
                @Index(name = "idx_btm_org", columnList = "organization_code")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusinessTemplateMapping extends PrimaryKeyEntity {

    @Column(name = "business_feature_code", nullable = false, length = 100)
    private String businessFeatureCode;

    @Column(name = "organization_code", length = 64)
    private String organizationCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approval_template_id", nullable = false)
    private ApprovalTemplateRoot approvalTemplateRoot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "form_template_id", nullable = false)
    private DraftFormTemplate draftFormTemplate;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    private BusinessTemplateMapping(String businessFeatureCode,
                                    String organizationCode,
                                    ApprovalTemplateRoot approvalTemplateRoot,
                                    DraftFormTemplate draftFormTemplate,
                                    OffsetDateTime now) {
        this.businessFeatureCode = businessFeatureCode;
        this.organizationCode = organizationCode;
        this.approvalTemplateRoot = approvalTemplateRoot;
        this.draftFormTemplate = draftFormTemplate;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static BusinessTemplateMapping create(String businessFeatureCode,
                                                 String organizationCode,
                                                 ApprovalTemplateRoot approvalTemplateRoot,
                                                 DraftFormTemplate draftFormTemplate,
                                                 OffsetDateTime now) {
        return new BusinessTemplateMapping(businessFeatureCode, organizationCode, approvalTemplateRoot, draftFormTemplate, now);
    }

    public void updateTemplates(ApprovalTemplateRoot approvalTemplate,
                                DraftFormTemplate formTemplate,
                                boolean active,
                                OffsetDateTime now) {
        this.approvalTemplateRoot = approvalTemplate;
        this.draftFormTemplate = formTemplate;
        this.active = active;
        this.updatedAt = now;
    }

    public boolean applicableTo(String businessFeatureCode, String organizationCode) {
        if (!this.businessFeatureCode.equals(businessFeatureCode)) {
            return false;
        }
        if (this.organizationCode == null) {
            return true;
        }
        return this.organizationCode.equals(organizationCode);
    }
}
