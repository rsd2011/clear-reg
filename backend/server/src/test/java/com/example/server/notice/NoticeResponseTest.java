package com.example.server.notice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoticeResponseTest {

    @Test
    @DisplayName("Notice를 NoticeResponse로 변환하면 필드가 그대로 매핑된다")
    void convertsFromNotice() {
        Notice notice = new Notice();
        notice.setDisplayNumber("2025-0001");
        notice.setTitle("제목");
        notice.setContent("내용");
        notice.setSeverity(NoticeSeverity.WARNING);
        notice.setAudience(NoticeAudience.ADMIN);
        notice.setPinned(true);
        OffsetDateTime now = OffsetDateTime.now();
        notice.setPublishAt(now);
        notice.setExpireAt(now.plusDays(1));

        NoticeResponse response = NoticeResponse.from(notice);

        assertThat(response.displayNumber()).isEqualTo("2025-0001");
        assertThat(response.audience()).isEqualTo(NoticeAudience.ADMIN);
        assertThat(response.pinned()).isTrue();
        assertThat(response.publishAt()).isEqualTo(now);
    }
}
