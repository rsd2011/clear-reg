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

    @Test
    @DisplayName("null/타입 불일치/self 체크")
    void equalsNullTypeSelf() {
        DrmAuditEvent a = DrmAuditEvent.builder().assetId("id").eventType(DrmEventType.REQUEST).build();
        assertThat(a.equals(null)).isFalse();
        assertThat(a.equals("string")).isFalse();
        assertThat(a).isEqualTo(a); // self
    }

    @Test
    @DisplayName("각 필드가 다르면 equals는 false")
    void equalsDetectsAllFieldDifferences() {
        DrmAuditEvent base = fullEvent();

        // eventType
        DrmAuditEvent diff = DrmAuditEvent.builder()
                .assetId("a").eventType(DrmEventType.APPROVAL).reasonCode("R").reasonText("T")
                .requestorId("u").approverId("appr").expiresAt(Instant.parse("2025-01-01T00:00:00Z"))
                .route("r").tags(Set.of("T")).organizationCode("O").build();
        assertThat(base).isNotEqualTo(diff);

        // reasonCode
        diff = DrmAuditEvent.builder()
                .assetId("a").eventType(DrmEventType.REQUEST).reasonCode("R2").reasonText("T")
                .requestorId("u").approverId("appr").expiresAt(Instant.parse("2025-01-01T00:00:00Z"))
                .route("r").tags(Set.of("T")).organizationCode("O").build();
        assertThat(base).isNotEqualTo(diff);

        // reasonText
        diff = DrmAuditEvent.builder()
                .assetId("a").eventType(DrmEventType.REQUEST).reasonCode("R").reasonText("T2")
                .requestorId("u").approverId("appr").expiresAt(Instant.parse("2025-01-01T00:00:00Z"))
                .route("r").tags(Set.of("T")).organizationCode("O").build();
        assertThat(base).isNotEqualTo(diff);

        // requestorId
        diff = DrmAuditEvent.builder()
                .assetId("a").eventType(DrmEventType.REQUEST).reasonCode("R").reasonText("T")
                .requestorId("u2").approverId("appr").expiresAt(Instant.parse("2025-01-01T00:00:00Z"))
                .route("r").tags(Set.of("T")).organizationCode("O").build();
        assertThat(base).isNotEqualTo(diff);

        // approverId
        diff = DrmAuditEvent.builder()
                .assetId("a").eventType(DrmEventType.REQUEST).reasonCode("R").reasonText("T")
                .requestorId("u").approverId("appr2").expiresAt(Instant.parse("2025-01-01T00:00:00Z"))
                .route("r").tags(Set.of("T")).organizationCode("O").build();
        assertThat(base).isNotEqualTo(diff);

        // expiresAt
        diff = DrmAuditEvent.builder()
                .assetId("a").eventType(DrmEventType.REQUEST).reasonCode("R").reasonText("T")
                .requestorId("u").approverId("appr").expiresAt(Instant.parse("2025-01-02T00:00:00Z"))
                .route("r").tags(Set.of("T")).organizationCode("O").build();
        assertThat(base).isNotEqualTo(diff);

        // route
        diff = DrmAuditEvent.builder()
                .assetId("a").eventType(DrmEventType.REQUEST).reasonCode("R").reasonText("T")
                .requestorId("u").approverId("appr").expiresAt(Instant.parse("2025-01-01T00:00:00Z"))
                .route("r2").tags(Set.of("T")).organizationCode("O").build();
        assertThat(base).isNotEqualTo(diff);

        // organizationCode
        diff = DrmAuditEvent.builder()
                .assetId("a").eventType(DrmEventType.REQUEST).reasonCode("R").reasonText("T")
                .requestorId("u").approverId("appr").expiresAt(Instant.parse("2025-01-01T00:00:00Z"))
                .route("r").tags(Set.of("T")).organizationCode("O2").build();
        assertThat(base).isNotEqualTo(diff);
    }

    @Test
    @DisplayName("null 필드 조합 equals 브랜치 커버")
    void equalsHandlesNullFields() {
        DrmAuditEvent withNulls = DrmAuditEvent.builder()
                .assetId(null).eventType(null).reasonCode(null).reasonText(null)
                .requestorId(null).approverId(null).expiresAt(null)
                .route(null).tags(null).organizationCode(null).build();
        DrmAuditEvent withNulls2 = DrmAuditEvent.builder()
                .assetId(null).eventType(null).reasonCode(null).reasonText(null)
                .requestorId(null).approverId(null).expiresAt(null)
                .route(null).tags(null).organizationCode(null).build();

        assertThat(withNulls).isEqualTo(withNulls2);

        DrmAuditEvent withValue = DrmAuditEvent.builder()
                .assetId("id").eventType(null).reasonCode(null).reasonText(null)
                .requestorId(null).approverId(null).expiresAt(null)
                .route(null).tags(null).organizationCode(null).build();
        assertThat(withNulls).isNotEqualTo(withValue);
    }

    @Test
    @DisplayName("getTags() - null은 빈 Set 반환")
    void getTagsNullReturnsEmptySet() {
        DrmAuditEvent event = DrmAuditEvent.builder().assetId("id").tags(null).build();
        assertThat(event.getTags()).isEmpty();
    }

    @Test
    @DisplayName("Builder.tags(null)은 빈 Set으로 변환")
    void builderTagsNullConvertsToEmpty() {
        DrmAuditEvent event = DrmAuditEvent.builder().assetId("id").tags(null).build();
        assertThat(event.getTags()).isEmpty();
    }

    private DrmAuditEvent fullEvent() {
        return DrmAuditEvent.builder()
                .assetId("a").eventType(DrmEventType.REQUEST).reasonCode("R").reasonText("T")
                .requestorId("u").approverId("appr").expiresAt(Instant.parse("2025-01-01T00:00:00Z"))
                .route("r").tags(Set.of("T")).organizationCode("O").build();
    }
}
