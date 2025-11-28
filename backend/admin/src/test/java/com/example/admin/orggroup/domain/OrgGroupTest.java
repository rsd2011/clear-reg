package com.example.admin.orggroup.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OrgGroup/Category/Member 단순 엔터티 커버리지")
class OrgGroupTest {

    @Test
    void buildAndGetters() {
        // OrgGroup: sort 및 PermGroupCode 필드 테스트
        OrgGroup group = OrgGroup.builder()
            .code("GRP1")
            .name("영업그룹")
            .description("desc")
            .sort(10)
            .leaderPermGroupCode("LEAD")
            .managerPermGroupCode("MGR")
            .memberPermGroupCode("MEM")
            .build();
        assertThat(group.getCode()).isEqualTo("GRP1");
        assertThat(group.getSort()).isEqualTo(10);
        assertThat(group.getName()).isEqualTo("영업그룹");
        assertThat(group.getDescription()).isEqualTo("desc");
        assertThat(group.getLeaderPermGroupCode()).isEqualTo("LEAD");
        assertThat(group.getManagerPermGroupCode()).isEqualTo("MGR");
        assertThat(group.getMemberPermGroupCode()).isEqualTo("MEM");

        // sort가 null인 경우
        OrgGroup groupNullSort = OrgGroup.builder().code("GRP2").name("기본").build();
        assertThat(groupNullSort.getSort()).isNull();

        // OrgGroupCategory 테스트
        OrgGroupCategory cat = OrgGroupCategory.builder()
            .code("CAT1")
            .label("영업")
            .description("d")
            .build();
        assertThat(cat.getLabel()).isEqualTo("영업");
        assertThat(cat.getCode()).isEqualTo("CAT1");
        assertThat(cat.getDescription()).isEqualTo("d");

        // OrgGroupMember: sort만 가지고 PermGroupCode 필드 없음
        OrgGroupMember member = OrgGroupMember.builder()
            .groupCode("GRP1")
            .orgId("ORG1")
            .orgName("지점")
            .sort(5)
            .build();
        assertThat(member.getGroupCode()).isEqualTo("GRP1");
        assertThat(member.getSort()).isEqualTo(5);
        assertThat(member.getOrgId()).isEqualTo("ORG1");
        assertThat(member.getOrgName()).isEqualTo("지점");

        // sort가 null인 경우
        OrgGroupMember memberNullSort = OrgGroupMember.builder()
            .groupCode("GRP1")
            .orgId("ORG2")
            .build();
        assertThat(memberNullSort.getSort()).isNull();

        // no-args 생성자 커버리지
        OrgGroup emptyGroup = new OrgGroup();
        OrgGroupCategory emptyCat = new OrgGroupCategory();
        OrgGroupMember emptyMember = new OrgGroupMember();
        assertThat(emptyGroup).isNotNull();
        assertThat(emptyCat).isNotNull();
        assertThat(emptyMember).isNotNull();
    }
}
