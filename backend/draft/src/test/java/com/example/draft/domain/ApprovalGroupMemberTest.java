package com.example.draft.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApprovalGroupMemberTest {

    @Test
    @DisplayName("ApprovalGroupMember는 생성 시 필드를 보존하고 기본 active=true다")
    void preservesFieldsOnCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        ApprovalGroup group = ApprovalGroup.create("CODE", "이름", null, "ORG", null, now);

        ApprovalGroupMember member = ApprovalGroupMember.create("user1", "ORG1", null, now);
        member.attachTo(group);

        assertThat(member.getApprovalGroup()).isEqualTo(group);
        assertThat(member.getMemberUserId()).isEqualTo("user1");
        assertThat(member.getMemberOrgCode()).isEqualTo("ORG1");
        assertThat(member.isActive()).isTrue();
        assertThat(member.getCreatedAt()).isEqualTo(now);
        assertThat(member.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("activate/deactivate 호출로 active 플래그와 updatedAt이 변경된다")
    void togglesActiveFlag() {
        OffsetDateTime now = OffsetDateTime.now();
        ApprovalGroup group = ApprovalGroup.create("CODE", "이름", null, "ORG", null, now);
        ApprovalGroupMember member = ApprovalGroupMember.create("user1", "ORG1", "x>1", now);
        member.attachTo(group);

        member.update("ORG2", "x>2", false, now.plusSeconds(1));
        assertThat(member.isActive()).isFalse();
        assertThat(member.getMemberOrgCode()).isEqualTo("ORG2");
        assertThat(member.getConditionExpression()).isEqualTo("x>2");
        assertThat(member.getUpdatedAt()).isAfter(member.getCreatedAt());
    }
}
