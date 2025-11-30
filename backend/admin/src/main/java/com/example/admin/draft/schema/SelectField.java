package com.example.admin.draft.schema;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * 선택 필드 (드롭다운, 라디오 버튼).
 * <p>
 * 단일 선택 또는 다중 선택을 지원합니다.
 * 옵션은 정적 목록 또는 외부 API에서 동적으로 로드할 수 있습니다.
 * </p>
 *
 * @param name          필드 고유 식별자
 * @param label         필드 라벨
 * @param required      필수 여부
 * @param description   필드 설명
 * @param options       선택 옵션 목록
 * @param multiple      다중 선택 여부
 * @param searchable    검색 가능 여부
 * @param optionsSource 외부 옵션 소스 URL (동적 로딩용)
 * @param displayType   표시 유형 (dropdown, radio, button-group)
 */
@JsonTypeName("select")
public record SelectField(
        String name,
        String label,
        boolean required,
        String description,
        List<SelectOption> options,
        boolean multiple,
        boolean searchable,
        String optionsSource,
        String displayType
) implements FormField {

    public SelectField {
        if (displayType == null || displayType.isBlank()) {
            displayType = "dropdown";
        }
        if (options == null) {
            options = List.of();
        }
    }

    /**
     * 정적 옵션으로 생성.
     */
    public static SelectField of(String name, String label, boolean required, List<SelectOption> options) {
        return new SelectField(name, label, required, null, options, false, false, null, "dropdown");
    }

    /**
     * 동적 옵션 소스로 생성.
     */
    public static SelectField dynamic(String name, String label, boolean required, String optionsSource) {
        return new SelectField(name, label, required, null, List.of(), false, true, optionsSource, "dropdown");
    }

    @Override
    public String type() {
        return "select";
    }

    /**
     * 선택 옵션.
     *
     * @param value    옵션 값
     * @param label    옵션 라벨
     * @param disabled 비활성화 여부
     */
    public record SelectOption(
            String value,
            String label,
            boolean disabled
    ) {
        public static SelectOption of(String value, String label) {
            return new SelectOption(value, label, false);
        }
    }
}
