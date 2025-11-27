package com.example.draft.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.admin.approval.ApprovalLineTemplate;
import com.example.common.jpa.PrimaryKeyEntity;
import com.example.draft.domain.exception.DraftAccessDeniedException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "draft_template_presets",
        indexes = {
                @Index(name = "idx_dtp_business_org", columnList = "business_feature_code, organization_code"),
                @Index(name = "idx_dtp_active", columnList = "active")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DraftTemplatePreset extends PrimaryKeyEntity {

    @Column(name = "preset_code", nullable = false, unique = true, length = 100)
    private String presetCode;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "business_feature_code", nullable = false, length = 100)
    private String businessFeatureCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private TemplateScope scope = TemplateScope.ORGANIZATION;

    @Column(name = "organization_code", length = 64)
    private String organizationCode;

    @Column(name = "title_template", nullable = false, columnDefinition = "text")
    private String titleTemplate;

    @Column(name = "content_template", nullable = false, columnDefinition = "text")
    private String contentTemplate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "form_template_id", nullable = false)
    private DraftFormTemplate formTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_approval_template_id")
    private ApprovalLineTemplate defaultApprovalTemplate;

    @Column(name = "default_form_payload", nullable = false, columnDefinition = "text")
    private String defaultFormPayload;

    @Column(name = "variables_json", columnDefinition = "text")
    private String variablesJson;

    @Column(name = "version", nullable = false)
    private int version = 1;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    private DraftTemplatePreset(String name,
                                String businessFeatureCode,
                                TemplateScope scope,
                                String organizationCode,
                                String titleTemplate,
                                String contentTemplate,
                                DraftFormTemplate formTemplate,
                                ApprovalLineTemplate defaultApprovalTemplate,
                                String defaultFormPayload,
                                String variablesJson,
                                OffsetDateTime now) {
        this.presetCode = UUID.randomUUID().toString();
        this.name = name;
        this.businessFeatureCode = businessFeatureCode;
        this.scope = scope;
        this.organizationCode = organizationCode;
        this.titleTemplate = titleTemplate;
        this.contentTemplate = contentTemplate;
        this.formTemplate = formTemplate;
        this.defaultApprovalTemplate = defaultApprovalTemplate;
        this.defaultFormPayload = defaultFormPayload == null ? "{}" : defaultFormPayload;
        this.variablesJson = variablesJson;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static DraftTemplatePreset create(String name,
                                             String businessFeatureCode,
                                             String organizationCode,
                                             String titleTemplate,
                                             String contentTemplate,
                                             DraftFormTemplate formTemplate,
                                             ApprovalLineTemplate defaultApprovalTemplate,
                                             String defaultFormPayload,
                                             String variablesJson,
                                             boolean active,
                                             OffsetDateTime now) {
        TemplateScope scope = organizationCode == null ? TemplateScope.GLOBAL : TemplateScope.ORGANIZATION;
        DraftTemplatePreset preset = new DraftTemplatePreset(name, businessFeatureCode, scope, organizationCode, titleTemplate,
                contentTemplate, formTemplate, defaultApprovalTemplate, defaultFormPayload, variablesJson, now);
        preset.active = active;
        return preset;
    }

    public void update(String name,
                       String titleTemplate,
                       String contentTemplate,
                       DraftFormTemplate formTemplate,
                       ApprovalLineTemplate defaultApprovalTemplate,
                       String defaultFormPayload,
                       String variablesJson,
                       boolean active,
                       OffsetDateTime now) {
        this.name = name;
        this.titleTemplate = titleTemplate;
        this.contentTemplate = contentTemplate;
        this.formTemplate = formTemplate;
        this.defaultApprovalTemplate = defaultApprovalTemplate;
        this.defaultFormPayload = defaultFormPayload == null ? "{}" : defaultFormPayload;
        this.variablesJson = variablesJson;
        this.active = active;
        this.updatedAt = now;
        this.version += 1;
    }

    public void assertOrganization(String organizationCode) {
        if (scope == TemplateScope.ORGANIZATION && !this.organizationCode.equals(organizationCode)) {
            throw new DraftAccessDeniedException("조직별 템플릿 프리셋이 아닙니다.");
        }
    }

    public boolean matchesBusiness(String businessFeatureCode) {
        return this.businessFeatureCode.equalsIgnoreCase(businessFeatureCode);
    }

    public boolean isGlobal() {
        return scope == TemplateScope.GLOBAL;
    }
}
