package com.example.server.notice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NoticeResponse 변환/마스킹")
class NoticeResponseTest {

    @Test
    @DisplayName("Given Notice When from 호출 & 마스킹 함수 제공 Then 제목/번호/본문에 적용된다")
    void fromAppliesMasker() {
        Notice notice = new Notice();
        notice.setDisplayNumber("N-001");
        notice.setTitle("중요 공지");
        notice.setContent("비공개 내용");
        notice.setPublishAt(OffsetDateTime.now());
        notice.setExpireAt(OffsetDateTime.now().plusDays(1));

        AtomicInteger called = new AtomicInteger();
        NoticeResponse masked = NoticeResponse.from(notice, v -> {
            called.incrementAndGet();
            return "[MASK]";
        });

        assertThat(masked.displayNumber()).isEqualTo("[MASK]");
        assertThat(masked.title()).isEqualTo("[MASK]");
        assertThat(masked.content()).isEqualTo("[MASK]");
        assertThat(called.get()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Given 기존 NoticeResponse When apply 호출 & 마스킹 함수 제공 Then 필드에 재적용된다")
    void applyReusesExistingResponse() {
        NoticeResponse base = new NoticeResponse(
                UUID.randomUUID(),
                "N-002",
                "제목",
                "본문",
                NoticeSeverity.INFO,
                NoticeAudience.GLOBAL,
                false,
                OffsetDateTime.now(),
                OffsetDateTime.now().plusDays(2)
        );

        NoticeResponse masked = NoticeResponse.apply(base, v -> "***");

        assertThat(masked.title()).isEqualTo("***");
        assertThat(masked.displayNumber()).isEqualTo("***");
        assertThat(masked.content()).isEqualTo("***");
        // 다른 필드는 그대로 유지
        assertThat(masked.severity()).isEqualTo(base.severity());
        assertThat(masked.audience()).isEqualTo(base.audience());
    }

    @Test
    @DisplayName("Given 마스킹 함수 null When apply 호출 Then 원본 값이 유지된다")
    void applyWithNullMaskerKeepsValues() {
        NoticeResponse base = new NoticeResponse(
                UUID.randomUUID(),
                "N-003",
                "제목3",
                "본문3",
                NoticeSeverity.CRITICAL,
                NoticeAudience.ADMIN,
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now().plusDays(3)
        );

        NoticeResponse same = NoticeResponse.apply(base, null);

        assertThat(same).isEqualTo(base);
    }
}
