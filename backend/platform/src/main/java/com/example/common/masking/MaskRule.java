package com.example.common.masking;

import com.example.common.codegroup.annotation.ManagedCode;

@ManagedCode
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
