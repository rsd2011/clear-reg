package com.example.admin.draft.schema;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * 날짜/시간 입력 필드.
 * <p>
 * 날짜, 시간, 날짜+시간 입력을 지원합니다.
 * </p>
 *
 * @param name          필드 고유 식별자
 * @param label         필드 라벨
 * @param required      필수 여부
 * @param description   필드 설명
 * @param dateType      날짜 유형 (date, time, datetime)
 * @param minDate       최소 날짜
 * @param maxDate       최대 날짜
 * @param format        표시 형식 (예: yyyy-MM-dd)
 * @param includeTime   시간 포함 여부
 */
@JsonTypeName("date")
public record DateField(
        String name,
        String label,
        boolean required,
        String description,
        String dateType,
        LocalDate minDate,
        LocalDate maxDate,
        String format,
        boolean includeTime
) implements FormField {

    public DateField {
        if (dateType == null || dateType.isBlank()) {
            dateType = "date";
        }
    }

    /**
     * 간편 생성자.
     */
    public static DateField of(String name, String label, boolean required) {
        return new DateField(name, label, required, null, "date", null, null, null, false);
    }

    /**
     * 날짜+시간 필드 생성.
     */
    public static DateField datetime(String name, String label, boolean required) {
        return new DateField(name, label, required, null, "datetime", null, null, null, true);
    }

    @Override
    public String type() {
        return "date";
    }
}
