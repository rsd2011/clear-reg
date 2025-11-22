package com.example.draft.application.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftNotificationPayloadDelegatedTest {

    @Test
    @DisplayName("stepId와 delegatedTo가 있을 때 payload에 그대로 반영된다")
    void payloadStoresStepIdAndDelegated() {
        UUID draftId = UUID.randomUUID();
        UUID stepId = UUID.randomUUID();
        DraftNotificationPayload payload = new DraftNotificationPayload(
                draftId,
                "ACTION",
                "actor",
                "creator",
                "ORG",
                "FEATURE",
                stepId,
                "delegate",
                null,
                OffsetDateTime.now(),
                List.of("creator", "delegate")
        );

        assertThat(payload.stepId()).isEqualTo(stepId);
        assertThat(payload.delegatedTo()).isEqualTo("delegate");
        assertThat(payload.comment()).isNull();
    }
}
