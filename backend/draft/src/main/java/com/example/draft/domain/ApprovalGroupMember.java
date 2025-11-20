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
@Table(name = "approval_group_members",
        indexes = {
                @Index(name = "idx_agm_group", columnList = "group_id"),
                @Index(name = "idx_agm_user_org", columnList = "member_user_id, member_org_code")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalGroupMember extends PrimaryKeyEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ApprovalGroup approvalGroup;

    @Column(name = "member_user_id", nullable = false, length = 100)
    private String memberUserId;

    @Column(name = "member_org_code", length = 64)
    private String memberOrgCode;

    @Column(name = "condition_expression", length = 1000)
    private String conditionExpression;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    private ApprovalGroupMember(String memberUserId,
                                String memberOrgCode,
                                String conditionExpression,
                                OffsetDateTime now) {
        this.memberUserId = memberUserId;
        this.memberOrgCode = memberOrgCode;
        this.conditionExpression = conditionExpression;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static ApprovalGroupMember create(String memberUserId,
                                             String memberOrgCode,
                                             String conditionExpression,
                                             OffsetDateTime now) {
        return new ApprovalGroupMember(memberUserId, memberOrgCode, conditionExpression, now);
    }

    void attachTo(ApprovalGroup group) {
        this.approvalGroup = group;
    }

    public void update(String memberOrgCode, String conditionExpression, boolean active, OffsetDateTime now) {
        this.memberOrgCode = memberOrgCode;
        this.conditionExpression = conditionExpression;
        this.active = active;
        this.updatedAt = now;
    }
}
