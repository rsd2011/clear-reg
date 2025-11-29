package com.example.common.identifier;

import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

import com.example.common.masking.DataKind;
import com.example.common.masking.Maskable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 국제 주소 값 객체.
 * 필수: countryCode(ISO-3166 alpha-2), city, line1
 * 선택: stateOrProvince, line2, postalCode
 * toString/Json 직렬화 시 상세 주소는 마스킹하고 도시·국가만 노출한다.
 */
public final class Address implements Maskable<String> {

    private final String countryCode;
    private final String stateOrProvince;
    private final String city;
    private final String line1;
    private final String line2;
    private final String postalCode;

    private Address(String countryCode,
                    String stateOrProvince,
                    String city,
                    String line1,
                    String line2,
                    String postalCode) {
        this.countryCode = countryCode;
        this.stateOrProvince = stateOrProvince;
        this.city = city;
        this.line1 = line1;
        this.line2 = line2;
        this.postalCode = postalCode;
    }

    @JsonCreator
    public static Address of(String countryCode,
                             String stateOrProvince,
                             String city,
                             String line1,
                             String line2,
                             String postalCode) {
        String iso = normalizeCountry(countryCode);
        String normalizedCity = normalizeRequired(city, "city", 2, 100);
        String normalizedLine1 = normalizeRequired(line1, "line1", 2, 120);
        String normalizedState = normalizeOptional(stateOrProvince, "stateOrProvince", 2, 100);
        String normalizedLine2 = normalizeOptional(line2, "line2", 0, 120);
        String normalizedPostal = normalizeOptional(postalCode, "postalCode", 2, 32);
        return new Address(iso, normalizedState, normalizedCity, normalizedLine1, normalizedLine2, normalizedPostal);
    }

    private static String normalizeCountry(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("countryCode must not be blank");
        }
        String upper = code.trim().toUpperCase(Locale.ROOT);
        if (upper.length() != 2) {
            throw new IllegalArgumentException("countryCode must be ISO 3166-1 alpha-2");
        }
        boolean validIso = java.util.Arrays.stream(Locale.getISOCountries())
                .anyMatch(c -> c.equals(upper));
        if (!validIso) {
            throw new IllegalArgumentException("countryCode is not valid ISO 3166-1 alpha-2");
        }
        return upper;
    }

    private static String normalizeRequired(String value, String field, int min, int max) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() < min || trimmed.length() > max) {
            throw new IllegalArgumentException(field + " length must be between " + min + " and " + max);
        }
        return trimmed;
    }

    private static String normalizeOptional(String value, String field, int min, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() < min || trimmed.length() > max) {
            throw new IllegalArgumentException(field + " length must be between " + min + " and " + max);
        }
        return trimmed;
    }

    public String countryCode() {
        return countryCode;
    }

    public String stateOrProvince() {
        return stateOrProvince;
    }

    public String city() {
        return city;
    }

    public String line1() {
        return line1;
    }

    public String line2() {
        return line2;
    }

    public String postalCode() {
        return postalCode;
    }

    @Override
    public String raw() {
        return Stream.of(line1, line2, city, stateOrProvince, postalCode, countryCode)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    @Override
    public String masked() {
        String maskedDetail = "[ADDR-REDACTED]";
        return Stream.of(maskedDetail, city, stateOrProvince, countryCode)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .reduce((a, b) -> a + ", " + b)
                .orElse(maskedDetail);
    }

    @Override
    public DataKind dataKind() {
        return DataKind.ADDRESS;
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
        if (!(o instanceof Address address)) return false;
        return Objects.equals(countryCode, address.countryCode)
                && Objects.equals(stateOrProvince, address.stateOrProvince)
                && Objects.equals(city, address.city)
                && Objects.equals(line1, address.line1)
                && Objects.equals(line2, address.line2)
                && Objects.equals(postalCode, address.postalCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(countryCode, stateOrProvince, city, line1, line2, postalCode);
    }
}
