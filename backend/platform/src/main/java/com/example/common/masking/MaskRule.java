package com.example.common.masking;

public enum MaskRule {
    NONE, PARTIAL, FULL, HASH, TOKENIZE;

    public static MaskRule of(String value) {
        if (value == null) return NONE;
        try {
            return MaskRule.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return NONE;
        }
    }
}
