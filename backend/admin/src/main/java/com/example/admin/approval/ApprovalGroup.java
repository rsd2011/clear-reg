package com.example.admin.approval;

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
                @Index(name = "idx_approval_group_code", columnList = "group_code", unique = true)
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

    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    private long version;

    private ApprovalGroup(String groupCode,
                          String name,
                          String description,
                          Integer priority,
                          OffsetDateTime now) {
        this.groupCode = groupCode;
        this.name = name;
        this.description = description;
        this.priority = priority != null ? priority : 0;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static ApprovalGroup create(String groupCode,
                                       String name,
                                       String description,
                                       Integer priority,
                                       OffsetDateTime now) {
        return new ApprovalGroup(groupCode, name, description, priority, now);
    }

    public void rename(String name, String description, OffsetDateTime now) {
        this.name = name;
        this.description = description;
        this.updatedAt = now;
    }

    public void updatePriority(Integer priority, OffsetDateTime now) {
        this.priority = priority;
        this.updatedAt = now;
    }

    public void deactivate(OffsetDateTime now) {
        this.active = false;
        this.updatedAt = now;
    }

    public void activate(OffsetDateTime now) {
        this.active = true;
        this.updatedAt = now;
    }

    public boolean isActive() {
        return this.active;
    }
}
