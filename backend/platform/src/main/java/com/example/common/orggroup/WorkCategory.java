package com.example.common.orggroup;

import com.example.common.codegroup.annotation.ManagedCode;

/**
 * 업무 카테고리.
 *
 * <p>조직그룹이 담당하는 업무 분야를 나타낸다.</p>
 */
@ManagedCode
public enum WorkCategory {
    /** 준법 */
    COMPLIANCE,
    /** 영업 */
    SALES,
    /** 트레이딩 */
    TRADING,
    /** 리스크관리 */
    RISK_MANAGEMENT,
    /** 운영 */
    OPERATIONS;

    /**
     * 문자열로부터 WorkCategory를 반환한다.
     *
     * @param code 카테고리 코드
     * @return 해당 WorkCategory, 없으면 null
     */
    public static WorkCategory fromString(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
