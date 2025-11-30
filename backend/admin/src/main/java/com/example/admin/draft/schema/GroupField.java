package com.example.admin.draft.schema;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * 그룹(섹션) 필드.
 * <p>
 * 관련된 필드들을 논리적으로 그룹화합니다.
 * 폼 레이아웃의 섹션 구분에 사용됩니다.
 * </p>
 *
 * @param name        필드 고유 식별자
 * @param label       그룹 제목
 * @param required    필수 여부 (그룹 내 필수 필드 존재 시)
 * @param description 그룹 설명
 * @param fields      그룹 내 필드 목록
 * @param collapsible 접기/펼치기 가능 여부
 * @param collapsed   기본 접힘 상태
 */
@JsonTypeName("group")
public record GroupField(
        String name,
        String label,
        boolean required,
        String description,
        List<FormField> fields,
        boolean collapsible,
        boolean collapsed
) implements FormField {

    public GroupField {
        if (fields == null) {
            fields = List.of();
        }
    }

    /**
     * 간편 생성자.
     */
    public static GroupField of(String name, String label, List<FormField> fields) {
        return new GroupField(name, label, false, null, fields, false, false);
    }

    /**
     * 접기 가능한 그룹 생성.
     */
    public static GroupField collapsible(String name, String label, List<FormField> fields, boolean collapsed) {
        return new GroupField(name, label, false, null, fields, true, collapsed);
    }

    @Override
    public String type() {
        return "group";
    }
}
