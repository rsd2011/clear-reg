package com.example.draft.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Draft 도메인 값 객체 기본 동작")
class DraftDomainValueTest {

    @Test
    @DisplayName("DraftHistory.entry는 필드를 설정한다")
    void draftHistoryEntryAssignsFields() {
        Draft draft = new Draft();
        OffsetDateTime now = OffsetDateTime.now();
        DraftHistory history = DraftHistory.entry(draft, "SUBMITTED", "actor", "details", now);

        assertThat(history.getDraft()).isSameAs(draft);
        assertThat(history.getEventType()).isEqualTo("SUBMITTED");
        assertThat(history.getActor()).isEqualTo("actor");
        assertThat(history.getOccurredAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("DraftReference는 create/attach/deactivate를 지원한다")
    void draftReferenceLifecycle() {
        Draft draft = new Draft();
        OffsetDateTime now = OffsetDateTime.now();
        DraftReference ref = DraftReference.create("user1", "ORG", "adder", now);
        ref.attachTo(draft);
        ref.deactivate(now.plusDays(1));

        assertThat(ref.getDraft()).isSameAs(draft);
        assertThat(ref.isActive()).isFalse();
        assertThat(ref.getAddedAt()).isEqualTo(now.plusDays(1));
    }
}
