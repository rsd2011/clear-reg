package com.example.admin.rowaccesspolicy.domain;

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
import com.example.common.security.RowScope;

/**
 * 행 수준 접근 정책.
 * <p>
 * 사용자가 조회할 수 있는 데이터의 행 범위를 정의한다.
 * RowScope에 따라 자신의 데이터만(OWN), 조직 데이터(ORG), 전체 데이터(ALL) 등을 조회할 수 있다.
 */
@Entity
@Table(name = "row_access_policy")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RowAccessPolicy extends PrimaryKeyEntity {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RowScope rowScope;

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

    public boolean matches(FeatureCode feature, ActionCode action, String permGroup, Instant ts) {
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

        return isEffectiveAt(ts);
    }
}
