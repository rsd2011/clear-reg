package com.example.common.policy;

import java.util.Set;
import java.util.UUID;

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
     * Builder 확장.
     */
    public static class MaskingMatchBuilder {
        /**
         * 단일 DataKind를 Set으로 변환하여 설정.
         */
        public MaskingMatchBuilder dataKind(DataKind dataKind) {
            if (dataKind == null) {
                this.dataKinds = Set.of();
            } else {
                this.dataKinds = Set.of(dataKind);
            }
            return this;
        }
    }
}
