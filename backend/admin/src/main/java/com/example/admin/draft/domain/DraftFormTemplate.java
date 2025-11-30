package com.example.admin.draft.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.common.jpa.PrimaryKeyEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기안 양식 템플릿.
 *
 * <p>기안 생성 시 사용할 양식 스키마(JSON)를 정의한다.</p>
 */
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

    /**
     * 기안 양식 템플릿을 생성한다.
     *
     * @param name             이름
     * @param businessType     비즈니스 유형
     * @param organizationCode 조직 코드 (null이면 GLOBAL)
     * @param schemaJson       양식 스키마 JSON
     * @param now              생성 시점
     * @return 새로운 템플릿 인스턴스
     */
    public static DraftFormTemplate create(String name,
                                           String businessType,
                                           String organizationCode,
                                           String schemaJson,
                                           OffsetDateTime now) {
        TemplateScope scope = organizationCode == null ? TemplateScope.GLOBAL : TemplateScope.ORGANIZATION;
        return new DraftFormTemplate(UUID.randomUUID().toString(), name, businessType, scope, organizationCode, schemaJson, now);
    }

    /**
     * 조직 접근 권한을 검증한다.
     *
     * @param organizationCode 검증할 조직 코드
     * @throws IllegalStateException 조직별 템플릿인데 다른 조직에서 접근 시
     */
    public void assertOrganization(String organizationCode) {
        if (scope == TemplateScope.ORGANIZATION && !this.organizationCode.equals(organizationCode)) {
            throw new IllegalStateException("조직별 기안 양식이 아닙니다.");
        }
    }

    /**
     * 템플릿을 수정한다.
     *
     * @param name       새 이름
     * @param schemaJson 새 스키마 JSON
     * @param active     활성화 여부
     * @param now        수정 시점
     */
    public void update(String name, String schemaJson, boolean active, OffsetDateTime now) {
        this.name = name;
        this.schemaJson = schemaJson;
        this.active = active;
        this.version += 1;
        this.updatedAt = now;
    }

    /**
     * 비즈니스 유형이 일치하는지 확인한다.
     *
     * @param businessFeatureCode 비즈니스 기능 코드
     * @return 일치 여부
     */
    public boolean matchesBusiness(String businessFeatureCode) {
        return this.businessType.equalsIgnoreCase(businessFeatureCode);
    }

    /**
     * 전역 템플릿인지 확인한다.
     *
     * @return 전역 템플릿이면 true
     */
    public boolean isGlobal() {
        return scope == TemplateScope.GLOBAL;
    }
}
