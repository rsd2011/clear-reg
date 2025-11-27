package com.example.admin.codemanage.model;

import java.util.Locale;
import java.util.Optional;

public enum SystemCommonCodeType {
    NOTICE_CATEGORY("NOTICE_CATEGORY", CodeManageKind.DYNAMIC),
    FILE_CLASSIFICATION("FILE_CLASSIFICATION", CodeManageKind.STATIC),
    ALERT_CHANNEL("ALERT_CHANNEL", CodeManageKind.DYNAMIC),
    CUSTOM("CUSTOM", CodeManageKind.DYNAMIC);

    private final String code;
    private final CodeManageKind defaultKind;

    SystemCommonCodeType(String code, CodeManageKind defaultKind) {
        this.code = code;
        this.defaultKind = defaultKind;
    }

    public String code() {
        return code;
    }

    public CodeManageKind defaultKind() {
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
