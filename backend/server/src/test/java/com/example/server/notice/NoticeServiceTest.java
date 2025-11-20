package com.example.server.notice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({NoticeService.class, NoticeNumberGenerator.class, NoticeServiceTest.TestConfig.class})
@DisplayName("NoticeService 테스트")
class NoticeServiceTest {

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private NoticeRepository noticeRepository;

    @Test
    @DisplayName("Given 생성 요청 When 저장하면 Then 일련번호와 초안 상태가 설정된다")
    void givenCreateRequest_whenPersisted_thenDisplayNumberGenerated() {
        NoticeCreateRequest request = new NoticeCreateRequest(
                "제목",
                "내용",
                NoticeSeverity.INFO,
                NoticeAudience.GLOBAL,
                null,
                null,
                null);

        NoticeAdminResponse response = noticeService.createNotice(request, "tester");
        Notice persisted = noticeRepository.findById(response.id()).orElseThrow();

        assertThat(persisted.getDisplayNumber()).isEqualTo("2024-0001");
        assertThat(persisted.getStatus()).isEqualTo(NoticeStatus.DRAFT);
        assertThat(persisted.getCreatedBy()).isEqualTo("tester");
    }

    @Test
    @DisplayName("Given 초안 공지 When publish 호출 Then 상태가 반영되고 목록에 노출된다")
    void givenDraftNotice_whenPublish_thenStatusUpdated() {
        NoticeCreateRequest request = new NoticeCreateRequest(
                "제목",
                "내용",
                NoticeSeverity.CRITICAL,
                NoticeAudience.ADMIN,
                null,
                null,
                true);
        NoticeAdminResponse created = noticeService.createNotice(request, "tester");

        NoticePublishRequest publishRequest = new NoticePublishRequest(null, null, true);
        noticeService.publishNotice(created.id(), publishRequest, "tester");

        java.util.List<NoticeResponse> active = noticeService.listActiveNotices(NoticeAudience.ADMIN);
        assertThat(active).hasSize(1);
        NoticeResponse published = active.get(0);
        assertThat(published.displayNumber()).isEqualTo(created.displayNumber());
        assertThat(published.pinned()).isTrue();
        assertThat(published.publishAt()).isEqualTo(OffsetDateTime.ofInstant(TestConfig.NOW, ZoneOffset.UTC));
    }

    static class TestConfig {

        static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
