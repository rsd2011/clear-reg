package com.example.server.notice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoticeServiceListTest {

    @Mock
    private NoticeRepository noticeRepository;
    @Mock
    private NoticeNumberGenerator numberGenerator;

    private final Clock fixedClock = Clock.fixed(Instant.parse("2024-02-01T00:00:00Z"), ZoneOffset.UTC);

    private NoticeService service() {
        return new NoticeService(noticeRepository, numberGenerator, fixedClock);
    }

    @Test
    @DisplayName("audience가 null이면 GLOBAL로 조회한다")
    void listActiveNoticesUsesGlobalWhenNull() {
        service().listActiveNotices(null);

        verify(noticeRepository).findActiveNotices(eq(NoticeAudience.GLOBAL), any());
    }

    @Test
    @DisplayName("findActiveNotices가 빈 리스트를 반환하면 빈 리스트를 그대로 준다")
    void listActiveNoticesReturnsEmptyWhenRepoEmpty() {
        given(noticeRepository.findActiveNotices(eq(NoticeAudience.GLOBAL), any())).willReturn(List.of());

        List<NoticeResponse> result = service().listActiveNotices(null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findActiveNotices가 예외를 던지면 그대로 전파한다")
    void listActiveNoticesPropagatesRepositoryException() {
        given(noticeRepository.findActiveNotices(eq(NoticeAudience.GLOBAL), any()))
                .willThrow(new IllegalStateException("repo error"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service().listActiveNotices(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("repo error");
    }
}
