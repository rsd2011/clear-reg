package com.example.server.notice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Sort;

class NoticeServiceTest {

    Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
    NoticeRepository repository = Mockito.mock(NoticeRepository.class);
    NoticeNumberGenerator generator = Mockito.mock(NoticeNumberGenerator.class);
    NoticeService service = new NoticeService(repository, generator, clock);

    @Test
    @DisplayName("존재하지 않는 공지를 게시하면 예외를 던진다")
    void publishNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findLockedById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publishNotice(id, new NoticePublishRequest(null, null, false), "actor"))
                .isInstanceOf(NoticeNotFoundException.class);
    }

    @Test
    @DisplayName("아카이브된 공지를 게시하려 하면 상태 예외를 던진다")
    void publishArchived() {
        UUID id = UUID.randomUUID();
        Notice notice = new Notice();
        notice.setStatus(NoticeStatus.ARCHIVED);
        when(repository.findLockedById(id)).thenReturn(Optional.of(notice));

        assertThatThrownBy(() -> service.publishNotice(id, new NoticePublishRequest(null, null, false), "actor"))
                .isInstanceOf(NoticeStateException.class);
    }

    @Test
    @DisplayName("게시 성공 시 상태와 시간, 핀 여부를 업데이트한다")
    void publishSuccess() {
        UUID id = UUID.randomUUID();
        Notice notice = new Notice();
        when(repository.findLockedById(id)).thenReturn(Optional.of(notice));

        NoticeAdminResponse response = service.publishNotice(id, new NoticePublishRequest(null, null, true), "actor");

        assertThat(response.status()).isEqualTo(NoticeStatus.PUBLISHED);
        assertThat(response.pinned()).isTrue();
        assertThat(response.publishAt()).isEqualTo(OffsetDateTime.now(clock));
    }

    @Test
    @DisplayName("게시 중 리포지토리 조회가 예외면 그대로 전파한다")
    void publishPropagatesRepositoryError() {
        UUID id = UUID.randomUUID();
        when(repository.findLockedById(id)).thenThrow(new IllegalStateException("repo down"));

        assertThatThrownBy(() -> service.publishNotice(id, new NoticePublishRequest(null, null, true), "actor"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("repo down");
    }

    @Test
    @DisplayName("보관 요청 시 이미 보관된 공지는 다시 저장하지 않는다")
    void archiveReturnsWhenAlreadyArchived() {
        UUID id = UUID.randomUUID();
        Notice notice = new Notice();
        notice.setStatus(NoticeStatus.ARCHIVED);
        when(repository.findLockedById(id)).thenReturn(Optional.of(notice));

        service.archiveNotice(id, new NoticeArchiveRequest(null), "actor");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("활성 공지 조회 시 audience null이면 GLOBAL로 치환한다")
    void listActiveDefaultsAudience() {
        when(repository.findActiveNotices(NoticeAudience.GLOBAL, OffsetDateTime.now(clock))).thenReturn(Collections.emptyList());

        service.listActiveNotices(null);

        verify(repository).findActiveNotices(NoticeAudience.GLOBAL, OffsetDateTime.now(clock));
    }

    @Test
    @DisplayName("존재하지 않는 공지를 수정하면 예외가 발생한다")
    void updateNotFoundThrows() {
        UUID id = UUID.randomUUID();
        when(repository.findLockedById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateNotice(id, new NoticeUpdateRequest("t", "c", NoticeSeverity.INFO, NoticeAudience.ADMIN, null, null, true), "actor"))
                .isInstanceOf(NoticeNotFoundException.class);
    }

    @Test
    @DisplayName("보관된 공지는 수정 시 상태 예외를 던진다")
    void updateArchivedThrows() {
        UUID id = UUID.randomUUID();
        Notice notice = new Notice();
        notice.setStatus(NoticeStatus.ARCHIVED);
        when(repository.findLockedById(id)).thenReturn(Optional.of(notice));

        assertThatThrownBy(() -> service.updateNotice(id, new NoticeUpdateRequest("t", "c", NoticeSeverity.INFO, NoticeAudience.ADMIN, null, null, true), "actor"))
                .isInstanceOf(NoticeStateException.class);
    }

    @Test
    @DisplayName("공지 수정 시 필드를 갱신하고 응답을 반환한다")
    void updateSuccess() {
        UUID id = UUID.randomUUID();
        Notice notice = new Notice();
        notice.setStatus(NoticeStatus.DRAFT);
        when(repository.findLockedById(id)).thenReturn(Optional.of(notice));

        NoticeAdminResponse response = service.updateNotice(id,
                new NoticeUpdateRequest("new", "body", NoticeSeverity.WARNING, NoticeAudience.SECURITY,
                        OffsetDateTime.parse("2025-02-01T00:00:00Z"), OffsetDateTime.parse("2025-03-01T00:00:00Z"), false),
                "actor");

        assertThat(response.title()).isEqualTo("new");
        assertThat(response.severity()).isEqualTo(NoticeSeverity.WARNING);
        assertThat(response.audience()).isEqualTo(NoticeAudience.SECURITY);
    }

    @Test
    @DisplayName("publishAt이 지정되면 해당 시간으로 게시한다")
    void publishUsesProvidedTime() {
        UUID id = UUID.randomUUID();
        Notice notice = new Notice();
        OffsetDateTime publishAt = OffsetDateTime.parse("2025-02-01T00:00:00Z");
        OffsetDateTime expireAt = OffsetDateTime.parse("2025-03-01T00:00:00Z");
        when(repository.findLockedById(id)).thenReturn(Optional.of(notice));

        NoticeAdminResponse response = service.publishNotice(id, new NoticePublishRequest(publishAt, expireAt, false), "actor");

        assertThat(response.publishAt()).isEqualTo(publishAt);
        assertThat(response.expireAt()).isEqualTo(expireAt);
    }

    @Test
    @DisplayName("보관 시 expireAt이 없으면 현재 시간으로 설정한다")
    void archiveSetsExpireWhenAbsent() {
        UUID id = UUID.randomUUID();
        Notice notice = new Notice();
        notice.setStatus(NoticeStatus.PUBLISHED);
        when(repository.findLockedById(id)).thenReturn(Optional.of(notice));

        NoticeAdminResponse response = service.archiveNotice(id, new NoticeArchiveRequest(null), "actor");

        assertThat(response.expireAt()).isEqualTo(OffsetDateTime.now(clock));
        assertThat(response.pinned()).isFalse();
    }

    @Test
    @DisplayName("보관 요청에 시간이 있으면 해당 expireAt을 사용한다")
    void archiveUsesProvidedExpireAt() {
        UUID id = UUID.randomUUID();
        Notice notice = new Notice();
        notice.setStatus(NoticeStatus.PUBLISHED);
        OffsetDateTime expireAt = OffsetDateTime.parse("2025-04-01T00:00:00Z");
        when(repository.findLockedById(id)).thenReturn(Optional.of(notice));

        NoticeAdminResponse response = service.archiveNotice(id, new NoticeArchiveRequest(expireAt), "actor");

        assertThat(response.expireAt()).isEqualTo(expireAt);
    }

    @Test
    @DisplayName("정렬된 목록을 반환하도록 listNotices가 저장소 호출을 위임한다")
    void listNoticesDelegatesSort() {
        when(repository.findAll(any(Sort.class))).thenReturn(Collections.emptyList());

        service.listNotices();

        verify(repository).findAll(any(Sort.class));
    }
}
