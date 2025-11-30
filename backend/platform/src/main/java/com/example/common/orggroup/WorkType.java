package com.example.common.orggroup;

import com.example.common.codegroup.annotation.ManagedCode;

/**
 * 업무유형 코드.
 *
 * <p>기안 생성 시 업무유형에 따라 다른 승인선 템플릿을 적용할 수 있다.</p>
 */
@ManagedCode
public enum WorkType {

    /** 일반 업무 */
    GENERAL,

    /** 파일 반출 */
    FILE_EXPORT,

    /** 데이터 정정 */
    DATA_CORRECTION,

    /** 인사정보 변경 */
    HR_UPDATE,

    /** 정책 변경 */
    POLICY_CHANGE;

    /**
     * 문자열로 WorkType 조회 (대소문자 무시).
     *
     * @param code 코드 문자열
     * @return 매칭되는 WorkType, 없으면 null
     */
    public static WorkType fromString(String code) {
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
