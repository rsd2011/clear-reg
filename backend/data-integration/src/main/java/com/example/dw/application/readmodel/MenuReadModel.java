package com.example.dw.application.readmodel;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 메뉴 Read Model 스냅샷.
 */
public record MenuReadModel(
        String version,
        OffsetDateTime generatedAt,
        List<MenuItem> items
) {
}
