package com.example.common.masking;

import java.util.Set;

import lombok.Builder;
import lombok.Value;

/**
 * 마스킹 여부 판단에 필요한 컨텍스트.
 */
@Value
@Builder(toBuilder = true)
public class MaskingTarget {
    SubjectType subjectType; // CUSTOMER / EMPLOYEE / SYSTEM 등
    DataKind dataKind;       // 현재 처리 중인 민감 데이터 종류 (enum)
    boolean defaultMask;     // 정책 기본값
    boolean forceUnmask;     // 마스킹 해제 요청 (예: 승인된 해제)
    Set<DataKind> forceUnmaskKinds;  // 특정 데이터 종류만 해제
    Set<String> forceUnmaskFields;   // 특정 필드명(row의 컬럼/속성)만 해제
    Set<String> requesterRoles;      // 해제 요청자 역할/직무
    String rowId;            // 특정 row 기준 해제(옵션)
    String maskRule;         // NONE | PARTIAL | FULL | HASH | TOKENIZE 등
    String maskParams;       // JSON or 문자열 파라미터

    /**
     * 레거시 호환용: String dataKind를 받아 DataKind로 변환하는 빌더 메서드.
     */
    public static class MaskingTargetBuilder {
        /**
         * String 형태의 dataKind를 받아 DataKind enum으로 변환.
         * @deprecated DataKind enum을 직접 사용하세요.
         */
        @Deprecated
        public MaskingTargetBuilder dataKind(String dataKindStr) {
            this.dataKind = DataKind.fromString(dataKindStr);
            return this;
        }

        /**
         * DataKind enum을 직접 설정.
         */
        public MaskingTargetBuilder dataKind(DataKind dataKind) {
            this.dataKind = dataKind;
            return this;
        }

        /**
         * 레거시 호환용: Set&lt;String&gt;을 받아 Set&lt;DataKind&gt;로 변환.
         * @deprecated Set&lt;DataKind&gt;를 직접 사용하세요.
         */
        @Deprecated
        public MaskingTargetBuilder forceUnmaskKindsFromStrings(Set<String> kinds) {
            if (kinds == null || kinds.isEmpty()) {
                this.forceUnmaskKinds = Set.of();
            } else {
                this.forceUnmaskKinds = kinds.stream()
                        .map(DataKind::fromString)
                        .collect(java.util.stream.Collectors.toSet());
            }
            return this;
        }
    }

    /**
     * 레거시 호환용: dataKind를 String으로 반환.
     * @deprecated getDataKind()를 사용하세요.
     */
    @Deprecated
    public String getDataKindString() {
        return dataKind != null ? dataKind.name() : null;
    }
}
