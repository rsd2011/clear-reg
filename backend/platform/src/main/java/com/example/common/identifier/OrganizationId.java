package com.example.common.identifier;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 조직/법인/부서 식별자.
 */
public final class OrganizationId extends AbstractIdentifier {

    private final String raw;

    private OrganizationId(String raw) {
        this.raw = raw;
    }

    @JsonCreator
    public static OrganizationId of(String value) {
        String normalized = normalizeAndValidate(value, "OrganizationId");
        return new OrganizationId(normalized);
    }

    public String raw() {
        return raw;
    }

    public String masked() {
        return mask(raw);
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
}
