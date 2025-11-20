package com.example.draft.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.common.jpa.PrimaryKeyEntity;
import com.example.draft.domain.exception.DraftAccessDeniedException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "draft_form_templates",
        indexes = @Index(name = "idx_form_template_business_org", columnList = "business_type, organization_code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DraftFormTemplate extends PrimaryKeyEntity {

    @Column(name = "template_code", nullable = false, unique = true, length = 100)
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

    @Column(name = "schema_json", nullable = false, columnDefinition = "text")
    private String schemaJson;

    @Column(name = "version", nullable = false)
    private int version = 1;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    private DraftFormTemplate(String templateCode,
                              String name,
                              String businessType,
                              TemplateScope scope,
                              String organizationCode,
                              String schemaJson,
                              OffsetDateTime now) {
        this.templateCode = templateCode;
        this.name = name;
        this.businessType = businessType;
        this.scope = scope;
        this.organizationCode = organizationCode;
        this.schemaJson = schemaJson;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static DraftFormTemplate create(String name,
                                           String businessType,
                                           String organizationCode,
                                           String schemaJson,
                                           OffsetDateTime now) {
        TemplateScope scope = organizationCode == null ? TemplateScope.GLOBAL : TemplateScope.ORGANIZATION;
        return new DraftFormTemplate(UUID.randomUUID().toString(), name, businessType, scope, organizationCode, schemaJson, now);
    }

    public void assertOrganization(String organizationCode) {
        if (scope == TemplateScope.ORGANIZATION && !this.organizationCode.equals(organizationCode)) {
            throw new DraftAccessDeniedException("조직별 기안 양식이 아닙니다.");
        }
    }

    public void update(String name, String schemaJson, boolean active, OffsetDateTime now) {
        this.name = name;
        this.schemaJson = schemaJson;
        this.active = active;
        this.version += 1;
        this.updatedAt = now;
    }

    public boolean matchesBusiness(String businessFeatureCode) {
        return this.businessType.equalsIgnoreCase(businessFeatureCode);
    }

    public boolean isGlobal() {
        return scope == TemplateScope.GLOBAL;
    }
}
