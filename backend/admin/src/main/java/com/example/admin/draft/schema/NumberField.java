package com.example.admin.draft.schema;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * 숫자 입력 필드.
 * <p>
 * 정수, 실수, 금액 등의 숫자 입력을 지원합니다.
 * </p>
 *
 * @param name        필드 고유 식별자
 * @param label       필드 라벨
 * @param required    필수 여부
 * @param description 필드 설명
 * @param placeholder 플레이스홀더 텍스트
 * @param min         최소값
 * @param max         최대값
 * @param step        증가 단위
 * @param precision   소수점 자릿수
 * @param currency    통화 코드 (금액 필드인 경우)
 */
@JsonTypeName("number")
public record NumberField(
        String name,
        String label,
        boolean required,
        String description,
        String placeholder,
        BigDecimal min,
        BigDecimal max,
        BigDecimal step,
        Integer precision,
        String currency
) implements FormField {

    /**
     * 간편 생성자.
     */
    public static NumberField of(String name, String label, boolean required) {
        return new NumberField(name, label, required, null, null, null, null, null, null, null);
    }

    /**
     * 범위 지정 생성자.
     */
    public static NumberField ofRange(String name, String label, boolean required,
                                       BigDecimal min, BigDecimal max) {
        return new NumberField(name, label, required, null, null, min, max, null, null, null);
    }

    @Override
    public String type() {
        return "number";
    }
}
