package com.example.admin.draft.schema;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * 배열(반복) 필드.
 * <p>
 * 동일한 구조의 항목을 여러 개 입력할 수 있습니다.
 * 예: 경력사항, 학력사항, 가족관계 등
 * </p>
 *
 * @param name        필드 고유 식별자
 * @param label       필드 라벨
 * @param required    필수 여부
 * @param description 필드 설명
 * @param itemFields  배열 항목의 필드 구조
 * @param minItems    최소 항목 수
 * @param maxItems    최대 항목 수
 * @param addLabel    항목 추가 버튼 라벨
 */
@JsonTypeName("array")
public record ArrayField(
        String name,
        String label,
        boolean required,
        String description,
        List<FormField> itemFields,
        Integer minItems,
        Integer maxItems,
        String addLabel
) implements FormField {

    public ArrayField {
        if (itemFields == null) {
            itemFields = List.of();
        }
        if (addLabel == null || addLabel.isBlank()) {
            addLabel = "항목 추가";
        }
    }

    /**
     * 간편 생성자.
     */
    public static ArrayField of(String name, String label, boolean required, List<FormField> itemFields) {
        return new ArrayField(name, label, required, null, itemFields, null, null, "항목 추가");
    }

    /**
     * 범위 지정 생성자.
     */
    public static ArrayField ofRange(String name, String label, boolean required,
                                      List<FormField> itemFields, int minItems, int maxItems) {
        return new ArrayField(name, label, required, null, itemFields, minItems, maxItems, "항목 추가");
    }

    @Override
    public String type() {
        return "array";
    }
}
