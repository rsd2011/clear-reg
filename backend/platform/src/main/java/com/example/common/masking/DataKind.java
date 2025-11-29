package com.example.common.masking;

/**
 * 민감 데이터 종류와 기본 마스킹 규칙을 정의하는 열거형.
 *
 * <p>각 데이터 종류는 기본 마스킹 규칙({@link MaskRule})을 가지며,
 * 정책에 의해 마스킹 적용 여부가 결정된다.
 */
public enum DataKind {

    /** 주민등록번호 */
    SSN(MaskRule.FULL),

    /** 전화번호 */
    PHONE(MaskRule.PARTIAL),

    /** 이메일 주소 */
    EMAIL(MaskRule.PARTIAL),

    /** 계좌번호 */
    ACCOUNT_NO(MaskRule.PARTIAL),

    /** 조직/법인명 */
    ORG_NAME(MaskRule.FULL),

    /** 개인 이름 */
    PERSON_NAME(MaskRule.PARTIAL),

    /** 주소 */
    ADDRESS(MaskRule.PARTIAL),

    /** 카드번호 */
    CARD_NO(MaskRule.PARTIAL),

    /** 여권번호 */
    PASSPORT_NO(MaskRule.PARTIAL),

    /** 운전면허번호 */
    DRIVER_LICENSE(MaskRule.PARTIAL),

    /** 사업자등록번호 */
    BUSINESS_REG_NO(MaskRule.PARTIAL),

    /** 법인등록번호 */
    CORP_REG_NO(MaskRule.PARTIAL),

    /** 증권계좌번호 */
    SECURITIES_NO(MaskRule.PARTIAL),

    /** 고객ID */
    CUSTOMER_ID(MaskRule.PARTIAL),

    /** 사원ID */
    EMPLOYEE_ID(MaskRule.PARTIAL),

    /** 조직ID */
    ORGANIZATION_ID(MaskRule.PARTIAL),

    /** 기본값 (분류 불가 시) */
    DEFAULT(MaskRule.FULL);

    private final MaskRule defaultMaskRule;

    DataKind(MaskRule defaultMaskRule) {
        this.defaultMaskRule = defaultMaskRule;
    }

    /**
     * 이 데이터 종류의 기본 마스킹 규칙을 반환한다.
     *
     * @return 기본 마스킹 규칙
     */
    public MaskRule getDefaultMaskRule() {
        return defaultMaskRule;
    }

    /**
     * 문자열로부터 DataKind를 찾는다.
     * 대소문자를 무시하며, 찾지 못하면 DEFAULT를 반환한다.
     *
     * @param value 데이터 종류 문자열
     * @return 해당하는 DataKind, 없으면 DEFAULT
     */
    public static DataKind fromString(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT;
        }
        String normalized = value.trim().toUpperCase().replace("-", "_");
        try {
            return DataKind.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return DEFAULT;
        }
    }
}
