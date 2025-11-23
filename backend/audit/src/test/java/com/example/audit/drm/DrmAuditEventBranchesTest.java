package com.example.audit.drm;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DrmAuditEventBranchesTest {

    @Test
    @DisplayName("동일 필드면 equals/hashCode가 일치하고 assetId가 다르면 false")
    void equalsTrueAndAssetDiffFalse() {
        DrmAuditEvent a = DrmAuditEvent.builder()
                .assetId("file1")
                .eventType(DrmEventType.REQUEST)
                .reasonCode("R1")
                .reasonText("download")
                .requestorId("u1")
                .approverId("a1")
                .expiresAt(Instant.parse("2025-01-01T00:00:00Z"))
                .route("download")
                .tags(Set.of("DRM", "EXPORT"))
                .organizationCode("ORG1")
                .build();
        DrmAuditEvent b = DrmAuditEvent.builder()
                .assetId("file1")
                .eventType(DrmEventType.REQUEST)
                .reasonCode("R1")
                .reasonText("download")
                .requestorId("u1")
                .approverId("a1")
                .expiresAt(Instant.parse("2025-01-01T00:00:00Z"))
                .route("download")
                .tags(Set.of("DRM", "EXPORT"))
                .organizationCode("ORG1")
                .build();
        DrmAuditEvent c = DrmAuditEvent.builder()
                .assetId("file2")
                .eventType(DrmEventType.REQUEST)
                .reasonCode("R1")
                .reasonText("download")
                .requestorId("u1")
                .approverId("a1")
                .expiresAt(Instant.parse("2025-01-01T00:00:00Z"))
                .route("download")
                .tags(Set.of("DRM", "EXPORT"))
                .organizationCode("ORG1")
                .build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(c);
    }

    @Test
    @DisplayName("tags가 다르면 equals가 false가 된다")
    void equalsFalseWhenTagsDiffer() {
        DrmAuditEvent a = DrmAuditEvent.builder()
                .assetId("file1")
                .eventType(DrmEventType.REQUEST)
                .tags(Set.of("A"))
                .build();
        DrmAuditEvent b = DrmAuditEvent.builder()
                .assetId("file1")
                .eventType(DrmEventType.REQUEST)
                .tags(Set.of("B"))
                .build();

        assertThat(a).isNotEqualTo(b);
    }
}
