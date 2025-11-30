package com.example.admin.draft.schema;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * 텍스트 입력 필드.
 * <p>
 * 단일 행 텍스트, 다중 행 텍스트(textarea), 이메일, URL 등을 지원합니다.
 * </p>
 *
 * @param name        필드 고유 식별자
 * @param label       필드 라벨
 * @param required    필수 여부
 * @param description 필드 설명
 * @param placeholder 플레이스홀더 텍스트
 * @param minLength   최소 길이
 * @param maxLength   최대 길이
 * @param pattern     정규식 패턴 (validation용)
 * @param multiline   다중 행 여부 (textarea)
 * @param inputType   입력 타입 (text, email, url, tel 등)
 */
@JsonTypeName("text")
public record TextField(
        String name,
        String label,
        boolean required,
        String description,
        String placeholder,
        Integer minLength,
        Integer maxLength,
        String pattern,
        boolean multiline,
        String inputType
) implements FormField {

    public TextField {
        if (inputType == null || inputType.isBlank()) {
            inputType = "text";
        }
    }

    /**
     * 간편 생성자.
     */
    public static TextField of(String name, String label, boolean required) {
        return new TextField(name, label, required, null, null, null, null, null, false, "text");
    }

    /**
     * 다중 행 텍스트 필드 생성 (textarea).
     */
    public static TextField multiline(String name, String label, boolean required) {
        return new TextField(name, label, required, null, null, null, null, null, true, "text");
    }

    /**
     * 이메일 입력 필드 생성.
     */
    public static TextField email(String name, String label, boolean required) {
        return new TextField(name, label, required, null, null, null, null, null, false, "email");
    }

    /**
     * URL 입력 필드 생성.
     */
    public static TextField url(String name, String label, boolean required) {
        return new TextField(name, label, required, null, null, null, null, null, false, "url");
    }

    /**
     * 전화번호 입력 필드 생성.
     */
    public static TextField tel(String name, String label, boolean required) {
        return new TextField(name, label, required, null, null, null, null, null, false, "tel");
    }

    @Override
    public String type() {
        return "text";
    }
}
