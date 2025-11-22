package com.example.draft.application.notification;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftNotificationEventListenerTest {

    @Test
    @DisplayName("채널이 없으면 경고만 로깅하고 전파하지 않는다")
    void skipsWhenNoChannelsConfigured() {
        DraftNotificationPayload payload = samplePayload();

        DraftNotificationEventListener listener = new DraftNotificationEventListener(List.of());

        assertThatCode(() -> listener.onNotification(payload)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("채널이 있으면 모든 채널에 payload를 전파한다")
    void dispatchesToAllChannels() {
        DraftNotificationChannel channel1 = mock(DraftNotificationChannel.class);
        DraftNotificationChannel channel2 = mock(DraftNotificationChannel.class);
        DraftNotificationPayload payload = samplePayload();

        DraftNotificationEventListener listener = new DraftNotificationEventListener(List.of(channel1, channel2));

        listener.onNotification(payload);

        verify(channel1).send(payload);
        verify(channel2).send(payload);
    }

    private DraftNotificationPayload samplePayload() {
        return new DraftNotificationPayload(
                UUID.randomUUID(),
                "SUBMITTED",
                "actor",
                "creator",
                "ORG",
                "FEATURE",
                null,
                null,
                null,
                OffsetDateTime.now(),
                List.of("creator")
        );
    }
}
