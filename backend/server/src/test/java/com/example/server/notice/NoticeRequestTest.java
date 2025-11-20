package com.example.server.notice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Notice 요청 DTO 테스트")
class NoticeRequestTest {

    @Test
    @DisplayName("Given 요청 DTO When 생성하면 Then 필드가 정상 세팅된다")
    void createRequests() {
        OffsetDateTime now = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

        NoticeCreateRequest createRequest = new NoticeCreateRequest(
                "제목", "내용", NoticeSeverity.INFO, NoticeAudience.GLOBAL,
                now, now.plusDays(1), true);
        assertThat(createRequest.title()).isEqualTo("제목");

        NoticeUpdateRequest updateRequest = new NoticeUpdateRequest(
                "수정", "수정 내용", NoticeSeverity.WARNING, NoticeAudience.ADMIN,
                now, now.plusDays(2), false);
        assertThat(updateRequest.title()).isEqualTo("수정");

        NoticePublishRequest publishRequest = new NoticePublishRequest(now, now.plusDays(3), true);
        assertThat(publishRequest.publishAt()).isEqualTo(now);

        NoticeArchiveRequest archiveRequest = new NoticeArchiveRequest(now.plusDays(10));
        assertThat(archiveRequest.expireAt()).isEqualTo(now.plusDays(10));
    }
}
