package com.example.admin.maskingpolicy.domain;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import com.example.common.masking.DataKind;
import com.example.common.masking.DataKindSetConverter;

/**
 * 필드 수준 마스킹 정책.
 * <p>
 * 민감 데이터 필드에 대한 마스킹 규칙을 정의한다.
 * dataKinds별로 마스킹 여부를 설정할 수 있다.
 * 빈 Set이면 모든 데이터 종류에 적용됨.
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
     * 빈 Set이면 해당 조건의 모든 데이터에 적용.
     * JSON 형태로 저장: ["SSN", "PHONE"]
     */
    @Column(name = "data_kinds", columnDefinition = "TEXT")
    @Convert(converter = DataKindSetConverter.class)
    @Builder.Default
    private Set<DataKind> dataKinds = new LinkedHashSet<>();

    /**
     * 마스킹 적용 여부.
     * true: 마스킹 적용 (기본값)
     * false: 마스킹 해제 (화이트리스트)
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean maskingEnabled = true;

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

    /**
     * 지정된 DataKind가 이 정책에 포함되는지 확인.
     * dataKinds가 비어있으면 모든 종류에 적용됨을 의미.
     */
    public boolean appliesTo(DataKind kind) {
        if (dataKinds == null || dataKinds.isEmpty()) {
            return true; // 빈 Set = 모든 종류에 적용
        }
        return kind != null && dataKinds.contains(kind);
    }

    /**
     * 레거시 API 호환성을 위한 빌더 확장.
     */
    public static class MaskingPolicyBuilder {
        // Lombok이 생성하는 필드를 직접 선언하여 레거시 메서드에서 접근 가능하게 함
        private Boolean maskingEnabled$value;
        private boolean maskingEnabled$set;
        private Set<DataKind> dataKinds$value;
        private boolean dataKinds$set;

        /**
         * 단일 String dataKind를 받아 Set으로 변환.
         * @deprecated dataKinds(Set) 사용을 권장합니다.
         */
        @Deprecated
        public MaskingPolicyBuilder dataKind(String dataKind) {
            if (dataKind == null || dataKind.isBlank()) {
                this.dataKinds$value = new LinkedHashSet<>();
            } else {
                this.dataKinds$value = new LinkedHashSet<>();
                this.dataKinds$value.add(DataKind.fromString(dataKind));
            }
            this.dataKinds$set = true;
            return this;
        }

        /**
         * String Set을 DataKind Set으로 변환.
         * @deprecated dataKinds(Set&lt;DataKind&gt;) 사용을 권장합니다.
         */
        @Deprecated
        public MaskingPolicyBuilder dataKindsFromStrings(Set<String> kinds) {
            if (kinds == null || kinds.isEmpty()) {
                this.dataKinds$value = new LinkedHashSet<>();
            } else {
                this.dataKinds$value = kinds.stream()
                        .map(DataKind::fromString)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
            }
            this.dataKinds$set = true;
            return this;
        }

        /**
         * @deprecated maskRule 필드는 제거되었습니다. maskingEnabled를 사용하세요.
         */
        @Deprecated
        public MaskingPolicyBuilder maskRule(String maskRule) {
            this.maskingEnabled$value = maskRule != null && !"NONE".equalsIgnoreCase(maskRule);
            this.maskingEnabled$set = true;
            return this;
        }

        /**
         * @deprecated maskParams 필드는 제거되었습니다.
         */
        @Deprecated
        public MaskingPolicyBuilder maskParams(String maskParams) {
            // 무시 - 더 이상 사용되지 않음
            return this;
        }
    }

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

    /**
     * 레거시 호환: 단일 dataKind 반환 (첫 번째 값).
     * @deprecated getDataKinds() 사용을 권장합니다.
     */
    @Deprecated
    public String getDataKind() {
        if (dataKinds == null || dataKinds.isEmpty()) {
            return null;
        }
        return dataKinds.iterator().next().name();
    }

    /**
     * @deprecated maskRule 필드는 제거되었습니다. maskingEnabled를 사용하세요.
     */
    @Deprecated
    public String getMaskRule() {
        return Boolean.TRUE.equals(maskingEnabled) ? "PARTIAL" : "NONE";
    }

    /**
     * @deprecated maskParams 필드는 제거되었습니다.
     */
    @Deprecated
    public String getMaskParams() {
        return null;
    }

    /**
     * @deprecated matchDataKind(DataKind) 사용을 권장합니다.
     */
    @Deprecated
    public boolean matches(FeatureCode feature, ActionCode action, String permGroup, String kind, Instant ts) {
        return matches(feature, action, permGroup, DataKind.fromString(kind), ts);
    }

    public boolean matches(FeatureCode feature, ActionCode action, String permGroup, DataKind kind, Instant ts) {
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

        // dataKinds 매칭: 비어있으면 모든 종류에 적용
        if (!appliesTo(kind)) {
            return false;
        }

        return isEffectiveAt(ts);
    }
}
