package com.example.admin.draft.schema;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 폼 필드의 기본 인터페이스.
 * <p>
 * Jackson 다형성을 활용하여 JSON 직렬화/역직렬화 시 타입 정보를 보존합니다.
 * 각 필드 타입은 sealed interface를 통해 컴파일 타임에 검증됩니다.
 * </p>
 *
 * @see TextField
 * @see NumberField
 * @see DateField
 * @see SelectField
 * @see CheckboxField
 * @see FileField
 * @see ArrayField
 * @see GroupField
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextField.class, name = "text"),
        @JsonSubTypes.Type(value = NumberField.class, name = "number"),
        @JsonSubTypes.Type(value = DateField.class, name = "date"),
        @JsonSubTypes.Type(value = SelectField.class, name = "select"),
        @JsonSubTypes.Type(value = CheckboxField.class, name = "checkbox"),
        @JsonSubTypes.Type(value = FileField.class, name = "file"),
        @JsonSubTypes.Type(value = ArrayField.class, name = "array"),
        @JsonSubTypes.Type(value = GroupField.class, name = "group")
})
public sealed interface FormField
        permits TextField, NumberField, DateField, SelectField,
                CheckboxField, FileField, ArrayField, GroupField {

    /**
     * 필드의 고유 식별자.
     */
    String name();

    /**
     * 필드 라벨 (UI 표시용).
     */
    String label();

    /**
     * 필수 여부.
     */
    boolean required();

    /**
     * 필드 설명 (도움말).
     */
    String description();

    /**
     * 필드 타입 문자열 반환.
     */
    String type();
}
