package com.example.draft.domain;

import java.time.OffsetDateTime;

import com.example.common.jpa.PrimaryKeyEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "draft_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DraftHistory extends PrimaryKeyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draft_id", nullable = false)
    private Draft draft;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "actor", length = 100)
    private String actor;

    @Column(name = "details", length = 2000)
    private String details;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    private DraftHistory(Draft draft, String eventType, String actor, String details, OffsetDateTime occurredAt) {
        this.draft = draft;
        this.eventType = eventType;
        this.actor = actor;
        this.details = details;
        this.occurredAt = occurredAt;
    }

    public static DraftHistory entry(Draft draft,
                                     String eventType,
                                     String actor,
                                     String details,
                                     OffsetDateTime occurredAt) {
        return new DraftHistory(draft, eventType, actor, details, occurredAt);
    }
}
