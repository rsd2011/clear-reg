package com.example.admin.codegroup.util;

import java.util.Locale;

/**
 * 코드 그룹 관련 유틸리티.
 */
public final class CodeGroupUtils {

    private CodeGroupUtils() {
        // utility class
    }

    /**
     * Enum 클래스명(PascalCase)을 그룹 코드(UPPER_SNAKE_CASE)로 변환.
     *
     * <p>예시:</p>
     * <ul>
     *   <li>ApprovalStatus → APPROVAL_STATUS</li>
     *   <li>DraftType → DRAFT_TYPE</li>
     *   <li>HTTPStatus → HTTP_STATUS</li>
     *   <li>XMLParser → XML_PARSER</li>
     * </ul>
     *
     * @param className Enum 클래스명
     * @return UPPER_SNAKE_CASE 형식의 그룹 코드
     */
    public static String toGroupCode(String className) {
        if (className == null || className.isEmpty()) {
            return className;
        }

        StringBuilder result = new StringBuilder();
        char[] chars = className.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            if (Character.isUpperCase(c)) {
                // 첫 글자가 아니고, 이전 글자가 소문자이거나
                // 다음 글자가 소문자인 경우 언더스코어 추가
                if (i > 0 && (Character.isLowerCase(chars[i - 1]) ||
                        (i + 1 < chars.length && Character.isLowerCase(chars[i + 1])))) {
                    result.append('_');
                }
            }
            result.append(Character.toUpperCase(c));
        }

        return result.toString();
    }

    /**
     * Enum 클래스에서 그룹 코드 추출.
     *
     * @param enumClass Enum 클래스
     * @return UPPER_SNAKE_CASE 형식의 그룹 코드
     */
    public static String toGroupCode(Class<? extends Enum<?>> enumClass) {
        return toGroupCode(enumClass.getSimpleName());
    }

    /**
     * 문자열을 대문자 정규화.
     *
     * @param value 입력 문자열
     * @return 대문자로 변환된 문자열 (null이면 null 반환)
     */
    public static String normalize(String value) {
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }
}
