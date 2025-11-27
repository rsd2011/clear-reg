package com.example.server.notice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.server.notice.dto.NoticeAdminResponse;

@DisplayName("NoticeAdminResponse 변환/마스킹")
class NoticeAdminResponseTest {

    @Test
    @DisplayName("Given 마스킹 함수 null When apply 호출 Then 원본 NoticeAdminResponse를 유지한다")
    void applyWithNullMasker() {
        NoticeAdminResponse base = new NoticeAdminResponse(
                UUID.randomUUID(),
                "N-010",
                "제목",
                "내용",
                NoticeSeverity.INFO,
                NoticeAudience.GLOBAL,
                NoticeStatus.PUBLISHED,
                false,
                OffsetDateTime.now(),
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                "creator",
                "updater"
        );

        NoticeAdminResponse same = NoticeAdminResponse.apply(base, null);

        assertThat(same).isEqualTo(base);
    }
}
