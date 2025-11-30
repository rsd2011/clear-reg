package com.example.admin.draft.schema;

import java.util.List;
import java.util.Map;

/**
 * 폼 레이아웃 설정.
 * <p>
 * 필드의 배치, 그리드 설정, 섹션 구성 등을 정의합니다.
 * </p>
 *
 * @param columns      그리드 컬럼 수 (기본: 1)
 * @param sections     섹션 목록
 * @param fieldWidths  필드별 너비 설정 (필드명 -> 컬럼 span)
 * @param fieldOrder   필드 순서 (없으면 정의 순서)
 */
public record FormLayout(
        int columns,
        List<FormSection> sections,
        Map<String, Integer> fieldWidths,
        List<String> fieldOrder
) {
    public FormLayout {
        if (columns < 1) {
            columns = 1;
        }
        if (sections == null) {
            sections = List.of();
        }
        if (fieldWidths == null) {
            fieldWidths = Map.of();
        }
        if (fieldOrder == null) {
            fieldOrder = List.of();
        }
    }

    /**
     * 단일 컬럼 레이아웃 생성.
     */
    public static FormLayout singleColumn() {
        return new FormLayout(1, List.of(), Map.of(), List.of());
    }

    /**
     * 다중 컬럼 레이아웃 생성.
     */
    public static FormLayout columns(int columns) {
        return new FormLayout(columns, List.of(), Map.of(), List.of());
    }

    /**
     * 섹션 기반 레이아웃 생성.
     */
    public static FormLayout withSections(List<FormSection> sections) {
        return new FormLayout(1, sections, Map.of(), List.of());
    }

    /**
     * 폼 섹션.
     *
     * @param name        섹션 식별자
     * @param title       섹션 제목
     * @param description 섹션 설명
     * @param fieldNames  섹션에 포함된 필드명 목록
     * @param collapsible 접기/펼치기 가능 여부
     */
    public record FormSection(
            String name,
            String title,
            String description,
            List<String> fieldNames,
            boolean collapsible
    ) {
        public static FormSection of(String name, String title, List<String> fieldNames) {
            return new FormSection(name, title, null, fieldNames, false);
        }
    }
}
