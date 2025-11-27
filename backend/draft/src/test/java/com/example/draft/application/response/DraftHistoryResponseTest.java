package com.example.draft.application.response;

import com.example.draft.application.dto.*;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftHistory;

class DraftHistoryResponseTest {

    @Test
    @DisplayName("DraftHistoryResponse.from은 도메인 엔티티의 필드를 그대로 보존한다")
    void mapsFromDomain() {
        Draft draft = Draft.create("title", "content", "FEATURE", "ORG", "TEMPLATE", "actor", OffsetDateTime.now());
        DraftHistory history = DraftHistory.entry(draft, "SUBMITTED", "actor", "details", OffsetDateTime.now());

        DraftHistoryResponse response = DraftHistoryResponse.from(history);

        assertThat(response.id()).isEqualTo(history.getId());
        assertThat(response.eventType()).isEqualTo(history.getEventType());
        assertThat(response.actor()).isEqualTo(history.getActor());
        assertThat(response.details()).isEqualTo(history.getDetails());
        assertThat(response.occurredAt()).isEqualTo(history.getOccurredAt());
    }
}
