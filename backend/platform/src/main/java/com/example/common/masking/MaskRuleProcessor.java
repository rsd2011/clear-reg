package com.example.common.masking;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.UUID;

public final class MaskRuleProcessor {

    private MaskRuleProcessor() {}

    public static String apply(String rule, String value, String params) {
        if (value == null || rule == null) return value;
        String r = rule.toUpperCase(Locale.ROOT);
        return switch (r) {
            case "NONE" -> value;
            case "FULL" -> "[MASKED]";
            case "PARTIAL" -> partial(value);
            case "HASH" -> hash(value);
            case "TOKENIZE" -> tokenize(value);
            default -> value;
        };
    }

    private static String partial(String v) {
        if (v.length() <= 4) return "*".repeat(v.length());
        String head = v.substring(0, 2);
        String tail = v.substring(v.length() - 2);
        return head + "*".repeat(v.length() - 4) + tail;
    }

    private static String hash(String v) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(v.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "[HASH-ERROR]";
        }
    }

    private static String tokenize(String v) {
        return UUID.nameUUIDFromBytes(v.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
