package com.example.dw.application.readmodel;

import java.time.OffsetDateTime;
import java.util.List;

public record PermissionMenuReadModel(
        String version,
        OffsetDateTime generatedAt,
        List<PermissionMenuItem> items
) {
}
