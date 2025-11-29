package com.example.common.identifier;

import com.example.common.masking.DataKind;
import com.example.common.masking.Maskable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 임직원(내부 사용자) 식별자.
 */
public final class EmployeeId extends AbstractIdentifier implements Maskable<String> {

    private final String raw;

    private EmployeeId(String raw) {
        this.raw = raw;
    }

    @JsonCreator
    public static EmployeeId of(String value) {
        String normalized = normalizeAndValidate(value, "EmployeeId");
        return new EmployeeId(normalized);
    }

    @Override
    public String raw() {
        return raw;
    }

    @Override
    public String masked() {
        // 직원 ID는 앞 세 글자를 노출하고 나머지를 마스킹해 식별성을 유지한다.
        if (raw.length() <= 3) {
            return raw;
        }
        int visiblePrefix = Math.min(3, raw.length());
        int visibleSuffix = Math.min(2, raw.length() - visiblePrefix);
        String prefix = raw.substring(0, visiblePrefix);
        String suffix = raw.substring(raw.length() - visibleSuffix);
        String stars = "*".repeat(Math.max(0, raw.length() - visiblePrefix - visibleSuffix));
        return prefix + stars + suffix;
    }

    @Override
    public String toString() {
        return masked();
    }

    @JsonValue
    public String jsonValue() {
        return masked();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmployeeId that = (EmployeeId) o;
        return raw.equals(that.raw);
    }

    @Override
    public int hashCode() {
        return raw.hashCode();
    }

    @Override
    public DataKind dataKind() {
        return DataKind.EMPLOYEE_ID;
    }
}
