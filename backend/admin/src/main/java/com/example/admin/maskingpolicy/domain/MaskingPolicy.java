package com.example.admin.maskingpolicy.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.common.jpa.PrimaryKeyEntity;

/**
 * 필드 수준 마스킹 정책.
 * <p>
 * 민감 데이터 필드에 대한 마스킹 규칙을 정의한다.
 * dataKind별로 PARTIAL, FULL, HASH, TOKENIZE 등의 마스킹 규칙을 적용할 수 있다.
 */
@Entity
@Table(name = "masking_policy")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaskingPolicy extends PrimaryKeyEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private FeatureCode featureCode;

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private ActionCode actionCode;

    @Column(length = 100)
    private String permGroupCode;

    @Column(length = 100)
    private String orgGroupCode;

    /**
     * 민감 데이터 종류 (SSN, PHONE, EMAIL, ACCOUNT_NO 등).
     * null이면 해당 조건의 모든 데이터에 적용.
     */
    @Column(length = 100)
    private String dataKind;

    /**
     * 마스킹 규칙: NONE, PARTIAL, FULL, HASH, TOKENIZE.
     */
    @Column(nullable = false, length = 30)
    private String maskRule;

    /**
     * 마스킹 규칙에 대한 추가 파라미터 (JSON).
     * 예: {"visibleChars": 4, "maskChar": "*"}
     */
    @Column(columnDefinition = "TEXT")
    private String maskParams;

    /**
     * 마스킹/언마스킹 시 감사 로깅 여부.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean auditEnabled = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 100;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    private Instant effectiveFrom;
    private Instant effectiveTo;

    private Instant createdAt;
    private Instant updatedAt;

    public boolean isEffectiveAt(Instant ts) {
        if (Boolean.FALSE.equals(active)) {
            return false;
        }
        if (effectiveFrom != null && ts.isBefore(effectiveFrom)) {
            return false;
        }
        if (effectiveTo != null && !ts.isBefore(effectiveTo)) {
            return false;
        }
        return true;
    }

    public boolean matches(FeatureCode feature, ActionCode action, String permGroup, String kind, Instant ts) {
        if (!featureCode.equals(feature)) return false;

        if (actionCode != null) {
            if (action == null || !actionCode.equals(action)) return false;
        } else if (action != null) {
            return false;
        }

        if (permGroupCode != null) {
            if (permGroup == null || !permGroupCode.equals(permGroup)) return false;
        } else if (permGroup != null) {
            return false;
        }

        // dataKind 매칭: null이면 모든 종류에 적용, 값이 있으면 정확히 매칭
        if (dataKind != null) {
            if (kind == null || !dataKind.equalsIgnoreCase(kind)) return false;
        }

        return isEffectiveAt(ts);
    }
}
