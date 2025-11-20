package com.example.draft.domain;

import java.time.OffsetDateTime;

import com.example.common.jpa.PrimaryKeyEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "draft_references",
        indexes = {
                @Index(name = "idx_draft_ref_draft", columnList = "draft_id"),
                @Index(name = "idx_draft_ref_user", columnList = "referenced_user_id, referenced_org_code")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DraftReference extends PrimaryKeyEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "draft_id", nullable = false)
    private Draft draft;

    @Column(name = "referenced_user_id", nullable = false, length = 100)
    private String referencedUserId;

    @Column(name = "referenced_org_code", length = 64)
    private String referencedOrgCode;

    @Column(name = "added_by", nullable = false, length = 100)
    private String addedBy;

    @Column(name = "added_at", nullable = false)
    private OffsetDateTime addedAt;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    private DraftReference(String referencedUserId,
                           String referencedOrgCode,
                           String addedBy,
                           OffsetDateTime addedAt) {
        this.referencedUserId = referencedUserId;
        this.referencedOrgCode = referencedOrgCode;
        this.addedBy = addedBy;
        this.addedAt = addedAt;
    }

    public static DraftReference create(String referencedUserId,
                                        String referencedOrgCode,
                                        String addedBy,
                                        OffsetDateTime addedAt) {
        return new DraftReference(referencedUserId, referencedOrgCode, addedBy, addedAt);
    }

    void attachTo(Draft draft) {
        this.draft = draft;
    }

    public void deactivate(OffsetDateTime now) {
        this.active = false;
        this.addedAt = now;
    }
}
