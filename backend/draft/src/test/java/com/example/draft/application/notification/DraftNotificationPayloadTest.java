package com.example.draft.application.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftNotificationPayloadTest {

    @Test
    @DisplayName("DraftNotificationPayload는 값 객체로 필드를 그대로 보존한다")
    void retainsValues() {
        OffsetDateTime now = OffsetDateTime.now();
        UUID draftId = UUID.randomUUID();
        DraftNotificationPayload payload = new DraftNotificationPayload(
                draftId,
                "SUBMITTED",
                "actor",
                    "creator",
                "ORG",
                "BF",
                UUID.randomUUID(),
                "delegate",
                "comment",
                now,
                List.of("r1", "r2")
        );

        assertThat(payload.draftId()).isEqualTo(draftId);
        assertThat(payload.action()).isEqualTo("SUBMITTED");
        assertThat(payload.organizationCode()).isEqualTo("ORG");
        assertThat(payload.recipients()).containsExactly("r1", "r2");
        assertThat(payload.occurredAt()).isEqualTo(now);
    }
}

