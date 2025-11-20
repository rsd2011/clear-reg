package com.example.dw.application.readmodel;

import java.util.Set;

public record PermissionMenuItem(
        String code,
        String name,
        String featureCode,
        String actionCode,
        String path,
        Set<String> maskingTags
) {
}
