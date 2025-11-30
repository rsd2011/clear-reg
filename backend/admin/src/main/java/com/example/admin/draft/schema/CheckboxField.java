package com.example.admin.draft.schema;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * 체크박스 필드.
 * <p>
 * 단일 boolean 값 또는 동의 확인용으로 사용됩니다.
 * </p>
 *
 * @param name         필드 고유 식별자
 * @param label        필드 라벨
 * @param required     필수 여부 (체크 필수)
 * @param description  필드 설명
 * @param defaultValue 기본값
 * @param checkLabel   체크박스 옆 라벨 (label과 별도)
 */
@JsonTypeName("checkbox")
public record CheckboxField(
        String name,
        String label,
        boolean required,
        String description,
        boolean defaultValue,
        String checkLabel
) implements FormField {

    /**
     * 간편 생성자.
     */
    public static CheckboxField of(String name, String label, boolean required) {
        return new CheckboxField(name, label, required, null, false, null);
    }

    /**
     * 동의 체크박스 생성.
     */
    public static CheckboxField agreement(String name, String checkLabel) {
        return new CheckboxField(name, null, true, null, false, checkLabel);
    }

    @Override
    public String type() {
        return "checkbox";
    }
}
