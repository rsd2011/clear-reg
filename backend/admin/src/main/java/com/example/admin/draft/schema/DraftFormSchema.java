package com.example.admin.draft.schema;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 기안 양식 스키마.
 * <p>
 * 기안 양식의 전체 구조를 정의합니다.
 * 필드 정의, 레이아웃, 첨부파일 설정, 기본값 등을 포함합니다.
 * </p>
 *
 * @param version          스키마 버전 (호환성 관리용)
 * @param fields           폼 필드 목록
 * @param layout           레이아웃 설정
 * @param attachmentConfig 첨부파일 설정
 * @param defaultValues    필드별 기본값
 * @param validationRules  추가 유효성 검사 규칙
 */
public record DraftFormSchema(
        String version,
        List<FormField> fields,
        FormLayout layout,
        AttachmentConfig attachmentConfig,
        Map<String, Object> defaultValues,
        List<ValidationRule> validationRules
) {
    private static final String CURRENT_VERSION = "1.0";

    public DraftFormSchema {
        if (version == null || version.isBlank()) {
            version = CURRENT_VERSION;
        }
        if (fields == null) {
            fields = List.of();
        }
        if (layout == null) {
            layout = FormLayout.singleColumn();
        }
        if (attachmentConfig == null) {
            attachmentConfig = AttachmentConfig.disabled();
        }
        if (defaultValues == null) {
            defaultValues = Map.of();
        }
        if (validationRules == null) {
            validationRules = List.of();
        }
    }

    /**
     * 필드 목록만으로 간편 생성.
     */
    public static DraftFormSchema of(List<FormField> fields) {
        return new DraftFormSchema(
                CURRENT_VERSION,
                fields,
                FormLayout.singleColumn(),
                AttachmentConfig.disabled(),
                Map.of(),
                List.of()
        );
    }

    /**
     * 필드와 레이아웃으로 생성.
     */
    public static DraftFormSchema of(List<FormField> fields, FormLayout layout) {
        return new DraftFormSchema(
                CURRENT_VERSION,
                fields,
                layout,
                AttachmentConfig.disabled(),
                Map.of(),
                List.of()
        );
    }

    /**
     * 빌더 패턴 시작.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * JSON 문자열로 변환.
     */
    public String toJson(ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize DraftFormSchema to JSON", e);
        }
    }

    /**
     * JSON 문자열에서 복원.
     */
    public static DraftFormSchema fromJson(String json, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(json, DraftFormSchema.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize DraftFormSchema from JSON", e);
        }
    }

    /**
     * 필드명으로 필드 조회.
     */
    public FormField findField(String name) {
        return fields.stream()
                .filter(f -> f.name().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * 빌더 클래스.
     */
    public static class Builder {
        private String version = CURRENT_VERSION;
        private List<FormField> fields = List.of();
        private FormLayout layout = FormLayout.singleColumn();
        private AttachmentConfig attachmentConfig = AttachmentConfig.disabled();
        private Map<String, Object> defaultValues = Map.of();
        private List<ValidationRule> validationRules = List.of();

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder fields(List<FormField> fields) {
            this.fields = fields;
            return this;
        }

        public Builder layout(FormLayout layout) {
            this.layout = layout;
            return this;
        }

        public Builder attachmentConfig(AttachmentConfig attachmentConfig) {
            this.attachmentConfig = attachmentConfig;
            return this;
        }

        public Builder defaultValues(Map<String, Object> defaultValues) {
            this.defaultValues = defaultValues;
            return this;
        }

        public Builder validationRules(List<ValidationRule> validationRules) {
            this.validationRules = validationRules;
            return this;
        }

        public DraftFormSchema build() {
            return new DraftFormSchema(version, fields, layout, attachmentConfig, defaultValues, validationRules);
        }
    }

    /**
     * 추가 유효성 검사 규칙.
     * <p>
     * 필드 간 관계 검증 등 복잡한 유효성 검사를 정의합니다.
     * </p>
     *
     * @param name       규칙 이름
     * @param type       규칙 타입 (required_if, min_sum, date_range 등)
     * @param fields     관련 필드명 목록
     * @param condition  조건 표현식
     * @param message    오류 메시지
     */
    public record ValidationRule(
            String name,
            String type,
            List<String> fields,
            String condition,
            String message
    ) {
        /**
         * 조건부 필수 규칙 생성.
         */
        public static ValidationRule requiredIf(String field, String condition, String message) {
            return new ValidationRule("required_if_" + field, "required_if", List.of(field), condition, message);
        }
    }
}
