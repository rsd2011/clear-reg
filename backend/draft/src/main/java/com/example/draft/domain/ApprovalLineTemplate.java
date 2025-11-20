package com.example.draft.domain;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.example.common.jpa.PrimaryKeyEntity;
import com.example.draft.domain.exception.DraftAccessDeniedException;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "approval_line_templates",
        indexes = {
                @Index(name = "idx_template_business_org", columnList = "business_type, organization_code")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalLineTemplate extends PrimaryKeyEntity {

    @Column(name = "template_code", nullable = false, length = 100, unique = true)
    private String templateCode;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "business_type", nullable = false, length = 100)
    private String businessType;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private TemplateScope scope = TemplateScope.ORGANIZATION;

    @Column(name = "organization_code", length = 64)
    private String organizationCode;

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
                                 String businessType,
                                 TemplateScope scope,
                                 String organizationCode,
                                 OffsetDateTime now) {
        this.templateCode = templateCode;
        this.name = name;
        this.businessType = businessType;
        this.scope = scope;
        this.organizationCode = organizationCode;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static ApprovalLineTemplate create(String name,
                                              String businessType,
                                              String organizationCode,
                                              OffsetDateTime now) {
        TemplateScope scope = organizationCode == null ? TemplateScope.GLOBAL : TemplateScope.ORGANIZATION;
        return new ApprovalLineTemplate(UUID.randomUUID().toString(), name, businessType, scope, organizationCode, now);
    }

    public static ApprovalLineTemplate createGlobal(String name,
                                                    String businessType,
                                                    OffsetDateTime now) {
        return new ApprovalLineTemplate(UUID.randomUUID().toString(), name, businessType,
                TemplateScope.GLOBAL, null, now);
    }

    public void rename(String name, boolean active, OffsetDateTime now) {
        this.name = name;
        this.active = active;
        this.updatedAt = now;
    }

    public void addStep(int stepOrder, String approvalGroupCode, String description) {
        ApprovalTemplateStep step = new ApprovalTemplateStep(this, stepOrder, approvalGroupCode, description);
        this.steps.add(step);
        this.steps.sort(Comparator.comparingInt(ApprovalTemplateStep::getStepOrder));
    }

    public List<DraftApprovalStep> instantiateSteps() {
        return steps.stream()
                .sorted(Comparator.comparingInt(ApprovalTemplateStep::getStepOrder))
                .map(DraftApprovalStep::fromTemplate)
                .toList();
    }

    public void assertOrganization(String organizationCode) {
        if (this.scope == TemplateScope.ORGANIZATION && !this.organizationCode.equals(organizationCode)) {
            throw new DraftAccessDeniedException("조직 템플릿이 아닙니다.");
        }
    }

    public boolean isGlobal() {
        return this.scope == TemplateScope.GLOBAL;
    }

    public boolean applicableTo(String organizationCode) {
        return isGlobal() || this.organizationCode.equals(organizationCode);
    }
}
