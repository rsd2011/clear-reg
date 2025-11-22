package com.example.common.util;

import java.util.Objects;

import app.daissue.koroman.Koroman;
import app.daissue.koroman.Koroman.CasingOption;
import com.example.common.identifier.Address;
import com.example.common.identifier.PersonName;

/**
 * Korean → Romanization helper wrapping koroman defaults.
 */
public final class KoreanRomanizer {

    private KoreanRomanizer() {}

    public static String romanize(PersonName name) {
        if (name == null) return null;
        return Koroman.romanize(name.raw(), true, CasingOption.CAPITALIZE_WORDS);
    }

    public static String romanizeAddress(Address address) {
        if (address == null) return null;
        StringBuilder sb = new StringBuilder();
        append(sb, address.city());
        append(sb, address.line1());
        append(sb, address.line2());
        String core = sb.toString().trim();
        String romanized = core.isEmpty() ? "" : Koroman.romanize(core, true, CasingOption.CAPITALIZE_WORDS);
        if (address.stateOrProvince() != null) {
            romanized = romanized + (romanized.isEmpty() ? "" : ", ")
                    + Koroman.romanize(address.stateOrProvince(), true, CasingOption.CAPITALIZE_WORDS);
        }
        if (address.countryCode() != null) {
            romanized = romanized + (romanized.isEmpty() ? "" : ", ") + address.countryCode();
        }
        return romanized;
    }

    private static void append(StringBuilder sb, String part) {
        if (Objects.nonNull(part) && !part.isBlank()) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(part.trim());
        }
    }
}
