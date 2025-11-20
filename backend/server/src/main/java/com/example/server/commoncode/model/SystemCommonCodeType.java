package com.example.server.commoncode.model;

import java.util.Locale;
import java.util.Optional;

public enum SystemCommonCodeType {
    NOTICE_CATEGORY("NOTICE_CATEGORY", CommonCodeKind.DYNAMIC),
    FILE_CLASSIFICATION("FILE_CLASSIFICATION", CommonCodeKind.STATIC),
    ALERT_CHANNEL("ALERT_CHANNEL", CommonCodeKind.DYNAMIC),
    CUSTOM("CUSTOM", CommonCodeKind.DYNAMIC);

    private final String code;
    private final CommonCodeKind defaultKind;

    SystemCommonCodeType(String code, CommonCodeKind defaultKind) {
        this.code = code;
        this.defaultKind = defaultKind;
    }

    public String code() {
        return code;
    }

    public CommonCodeKind defaultKind() {
        return defaultKind;
    }

    public static Optional<SystemCommonCodeType> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        String normalized = code.toUpperCase(Locale.ROOT);
        for (SystemCommonCodeType type : values()) {
            if (type.code.equals(normalized)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
