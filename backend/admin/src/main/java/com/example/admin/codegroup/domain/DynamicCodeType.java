package com.example.admin.codegroup.domain;

/**
 * DB에서 관리되는 동적 공통코드 타입.
 * 관리자가 CRUD 가능하며, metadataJson을 통해 확장 가능.
 */
public enum DynamicCodeType {

    /**
     * 공지사항 카테고리
     */
    NOTICE_CATEGORY,

    /**
     * 알림 채널
     */
    ALERT_CHANNEL,

    /**
     * 사용자 정의 코드
     */
    CUSTOM;

    /**
     * 이 타입의 소스 반환
     */
    public CodeGroupSource getSource() {
        return CodeGroupSource.DYNAMIC_DB;
    }

    /**
     * 코드 타입 문자열로 조회
     */
    public static DynamicCodeType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (DynamicCodeType type : values()) {
            if (type.name().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 주어진 코드 타입이 동적 코드인지 확인
     */
    public static boolean isDynamicType(String codeType) {
        return fromCode(codeType) != null;
    }
}
