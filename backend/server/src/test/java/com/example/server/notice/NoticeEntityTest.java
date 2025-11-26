package com.example.server.notice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoticeEntityTest {

    @Test
    @DisplayName("null 값이 전달되면 기본 Severity/Audience/Status로 설정된다")
    void nullValuesSetDefaults() {
        Notice notice = Notice.createDraft("N-1", "title", "content", null, null,
                null, null, false, "actor", OffsetDateTime.now(ZoneOffset.UTC));

        assertThat(notice.getSeverity()).isEqualTo(NoticeSeverity.INFO);
        assertThat(notice.getAudience()).isEqualTo(NoticeAudience.GLOBAL);
        assertThat(notice.getStatus()).isEqualTo(NoticeStatus.DRAFT);
    }

    @Test
    @DisplayName("markCreated/markUpdated 호출 시 감사 필드가 채워진다")
    void markCreatedAndUpdated() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Notice notice = Notice.createDraft("N-1", "title", "content", NoticeSeverity.INFO, NoticeAudience.GLOBAL,
                null, null, false, "alice", now);
        assertThat(notice.getCreatedBy()).isEqualTo("alice");
        assertThat(notice.getUpdatedBy()).isEqualTo("alice");
        assertThat(notice.getCreatedAt()).isEqualTo(now);
        assertThat(notice.getUpdatedAt()).isEqualTo(now);

        OffsetDateTime later = now.plusMinutes(1);
        notice.markUpdated("bob", later);
        assertThat(notice.getUpdatedBy()).isEqualTo("bob");
        assertThat(notice.getUpdatedAt()).isEqualTo(later);
    }
}
