package com.example.server.notice;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NoticeServiceRepositoryFailureTest {

    Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
    NoticeRepository repository = Mockito.mock(NoticeRepository.class);
    NoticeNumberGenerator generator = Mockito.mock(NoticeNumberGenerator.class);
    NoticeService service = new NoticeService(repository, generator, clock);

    @Test
    @DisplayName("리포지토리 예외는 서비스에서 그대로 전파된다")
    void repositoryErrorIsPropagated() {
        UUID id = UUID.randomUUID();
        when(repository.findLockedById(id)).thenThrow(new IllegalStateException("repo error"));

        assertThatThrownBy(() -> service.updateNotice(id,
                new NoticeUpdateRequest("t", "c", NoticeSeverity.INFO, NoticeAudience.ADMIN,
                        null, null, true), "actor"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("repo error");
    }
}
