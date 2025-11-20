package com.example.draft.domain;

import java.time.OffsetDateTime;

import com.example.common.jpa.PrimaryKeyEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "approval_groups",
        indexes = {
                @Index(name = "idx_approval_group_code", columnList = "group_code", unique = true),
                @Index(name = "idx_approval_group_org", columnList = "organization_code")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalGroup extends PrimaryKeyEntity {

    @Column(name = "group_code", nullable = false, length = 64)
    private String groupCode;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "organization_code", nullable = false, length = 64)
    private String organizationCode;

    @Column(name = "condition_expression", length = 1000)
    private String conditionExpression;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    private long version;

    private ApprovalGroup(String groupCode,
                          String name,
                          String description,
                          String organizationCode,
                          String conditionExpression,
                          OffsetDateTime now) {
        this.groupCode = groupCode;
        this.name = name;
        this.description = description;
        this.organizationCode = organizationCode;
        this.conditionExpression = conditionExpression;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static ApprovalGroup create(String groupCode,
                                       String name,
                                       String description,
                                       String organizationCode,
                                       String conditionExpression,
                                       OffsetDateTime now) {
        return new ApprovalGroup(groupCode, name, description, organizationCode, conditionExpression, now);
    }

    public void rename(String name, String description, String conditionExpression, OffsetDateTime now) {
        this.name = name;
        this.description = description;
        this.conditionExpression = conditionExpression;
        this.updatedAt = now;
    }
}
