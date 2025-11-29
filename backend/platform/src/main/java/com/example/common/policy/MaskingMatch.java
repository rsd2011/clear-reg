package com.example.common.policy;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.example.common.masking.DataKind;

import lombok.Builder;
import lombok.Value;

/**
 * 마스킹 정책 매칭 결과.
 */
@Value
@Builder
public class MaskingMatch {
    UUID policyId;
    Set<DataKind> dataKinds;
    boolean maskingEnabled;
    boolean auditEnabled;
    Integer priority;

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
     * 레거시 호환: maskRule 필드.
     * @deprecated maskingEnabled 사용을 권장합니다.
     */
    @Deprecated
    public String getMaskRule() {
        return maskingEnabled ? "PARTIAL" : "NONE";
    }

    /**
     * 레거시 호환: maskParams 필드.
     * @deprecated 더 이상 사용되지 않습니다.
     */
    @Deprecated
    public String getMaskParams() {
        return null;
    }

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
     * 레거시 Builder 호환.
     */
    public static class MaskingMatchBuilder {
        /**
         * 단일 String dataKind를 받아 Set으로 변환.
         * @deprecated dataKinds(Set) 사용을 권장합니다.
         */
        @Deprecated
        public MaskingMatchBuilder dataKind(String dataKind) {
            if (dataKind == null || dataKind.isBlank()) {
                this.dataKinds = Set.of();
            } else {
                this.dataKinds = Set.of(DataKind.fromString(dataKind));
            }
            return this;
        }

        /**
         * @deprecated maskingEnabled 사용을 권장합니다.
         */
        @Deprecated
        public MaskingMatchBuilder maskRule(String maskRule) {
            this.maskingEnabled = !"NONE".equalsIgnoreCase(maskRule);
            return this;
        }

        /**
         * @deprecated 더 이상 사용되지 않습니다.
         */
        @Deprecated
        public MaskingMatchBuilder maskParams(String maskParams) {
            // 무시
            return this;
        }

        /**
         * String Set을 DataKind Set으로 변환.
         * @deprecated dataKinds(Set&lt;DataKind&gt;) 사용을 권장합니다.
         */
        @Deprecated
        public MaskingMatchBuilder dataKindsFromStrings(Set<String> kinds) {
            if (kinds == null || kinds.isEmpty()) {
                this.dataKinds = Set.of();
            } else {
                this.dataKinds = kinds.stream()
                        .map(DataKind::fromString)
                        .collect(Collectors.toSet());
            }
            return this;
        }
    }
}
