package com.example.draft.application.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftNotificationPayloadNullsTest {

    @Test
    @DisplayName("delegatedTo와 comment가 null이어도 값 객체가 그대로 보존된다")
    void payloadAllowsNullDelegatedAndComment() {
        UUID draftId = UUID.randomUUID();
        DraftNotificationPayload payload = new DraftNotificationPayload(
                draftId,
                "ACTION",
                "actor",
                "creator",
                "ORG",
                "FEATURE",
                null,
                null,
                null,
                OffsetDateTime.now(),
                List.of("creator", "actor")
        );

        assertThat(payload.delegatedTo()).isNull();
        assertThat(payload.comment()).isNull();
        assertThat(payload.recipients()).containsExactlyInAnyOrder("creator", "actor");
        assertThat(payload.draftId()).isEqualTo(draftId);
    }
}
