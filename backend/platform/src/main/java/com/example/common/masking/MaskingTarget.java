package com.example.common.masking;

import lombok.Builder;
import lombok.Value;

/**
 * 마스킹 여부 판단에 필요한 컨텍스트.
 */
@Value
@Builder(toBuilder = true)
public class MaskingTarget {
    SubjectType subjectType; // CUSTOMER / EMPLOYEE / SYSTEM 등
    String dataKind;       // RRN, CARD, PAYMENT_REFERENCE 등 자유 텍스트
    boolean defaultMask;   // 정책 기본값
    boolean forceUnmask;   // 마스킹 해제 요청 (예: 승인된 해제)
    java.util.Set<String> forceUnmaskKinds; // 특정 데이터 종류만 해제
    java.util.Set<String> forceUnmaskFields; // 특정 필드명(row의 컬럼/속성)만 해제
    java.util.Set<String> requesterRoles; // 해제 요청자 역할/직무
    String rowId; // 특정 row 기준 해제(옵션)
    String maskRule; // NONE | PARTIAL | FULL | HASH | TOKENIZE 등
    String maskParams; // JSON or 문자열 파라미터
}
