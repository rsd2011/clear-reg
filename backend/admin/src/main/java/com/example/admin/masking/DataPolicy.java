package com.example.admin.masking;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.example.common.jpa.PrimaryKeyEntity;

@Entity
@Table(name = "data_policy")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataPolicy extends PrimaryKeyEntity {

    @Column(nullable = false, length = 100)
    private String featureCode;

    @Column(length = 100)
    private String actionCode;

    @Column(length = 100)
    private String permGroupCode;

    private Long orgPolicyId;

    @Column(length = 100)
    private String orgGroupCode;

    @Column(length = 100)
    private String businessType;

    /**
     * 마스킹 대상 필드 태그 (예: SSN, PHONE, EMAIL).
     * null이면 해당 feature/action 조합의 기본 정책.
     */
    @Column(length = 100)
    private String sensitiveTag;

    @Column(nullable = false, length = 30)
    private String rowScope; // OWN | ORG | ORG_AND_DESC | ALL | CUSTOM

    @Lob
    private String rowScopeExpr;

    @Column(nullable = false, length = 30)
    private String defaultMaskRule; // NONE | PARTIAL | FULL | HASH | TOKENIZE

    @Column(columnDefinition = "TEXT")
    private String maskParams; // JSON string

    /**
     * 마스킹 해제에 필요한 최소 액션 코드.
     * 예: UNMASK - 해당 액션 이상의 권한이 있어야 원본 노출
     */
    @Column(length = 30)
    private String requiredActionCode;

    /**
     * 마스킹/언마스킹 시 감사 로깅 여부.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean auditEnabled = false;

    @Column(nullable = false)
    private Integer priority;

    @Column(nullable = false)
    private Boolean active;

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

    public boolean matches(String feature, String action, String permGroup, Instant ts) {
        if (!featureCode.equals(feature)) return false;

        // 정밀 매칭: 입력이 null이면 필드가 존재할 경우 불일치
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

        return isEffectiveAt(ts);
    }
}
