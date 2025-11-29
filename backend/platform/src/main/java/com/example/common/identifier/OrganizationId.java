package com.example.common.identifier;

import com.example.common.masking.DataKind;
import com.example.common.masking.Maskable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 조직/법인/부서 식별자.
 */
public final class OrganizationId extends AbstractIdentifier implements Maskable<String> {

    private final String raw;

    private OrganizationId(String raw) {
        this.raw = raw;
    }

    @JsonCreator
    public static OrganizationId of(String value) {
        String normalized = normalizeAndValidate(value, "OrganizationId");
        return new OrganizationId(normalized);
    }

    @Override
    public String raw() {
        return raw;
    }

    @Override
    public String masked() {
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
        OrganizationId that = (OrganizationId) o;
        return raw.equals(that.raw);
    }

    @Override
    public int hashCode() {
        return raw.hashCode();
    }

    @Override
    public DataKind dataKind() {
        return DataKind.ORGANIZATION_ID;
    }
}
