package com.example.server.notice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Notice 값 보존 및 기본값 분기")
class NoticeValueTest {

    @Test
    @DisplayName("severity/audience/status가 null이면 기본값으로 설정된다")
    void defaultsWhenNull() {
        Notice notice = new Notice();
        notice.setSeverity(null);
        notice.setAudience(null);
        notice.setStatus(null);

        assertThat(notice.getSeverity()).isEqualTo(NoticeSeverity.INFO);
        assertThat(notice.getAudience()).isEqualTo(NoticeAudience.GLOBAL);
        assertThat(notice.getStatus()).isEqualTo(NoticeStatus.DRAFT);
    }

    @Test
    @DisplayName("pinned 값은 setter로 보존된다")
    void pinnedPreserved() {
        Notice notice = new Notice();
        notice.setPinned(true);

        assertThat(notice.isPinned()).isTrue();
    }
}
