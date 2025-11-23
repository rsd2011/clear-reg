package com.example.policy.datapolicy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OrgGroup/Category/Member 단순 엔터티 커버리지")
class OrgGroupEntitiesTest {

    @Test
    void buildAndGetters() {
        OrgGroup group = OrgGroup.builder().code("GRP1").name("영업그룹").description("desc").priority(10).build();
        assertThat(group.getCode()).isEqualTo("GRP1");
        assertThat(group.getPriority()).isEqualTo(10);
        assertThat(group.getName()).isEqualTo("영업그룹");
        assertThat(group.getDescription()).isEqualTo("desc");
        OrgGroup groupDefault = OrgGroup.builder().code("GRP2").name("기본").build();
        assertThat(groupDefault.getPriority()).isEqualTo(100);

        OrgGroupCategory cat = OrgGroupCategory.builder().code("CAT1").label("영업").description("d").build();
        assertThat(cat.getLabel()).isEqualTo("영업");
        assertThat(cat.getCode()).isEqualTo("CAT1");
        assertThat(cat.getDescription()).isEqualTo("d");

        OrgGroupMember member = OrgGroupMember.builder().groupCode("GRP1").orgId("ORG1").orgName("지점")
                .leaderPermGroupCode("LEAD").memberPermGroupCode("MEM").priority(5).build();
        assertThat(member.getLeaderPermGroupCode()).isEqualTo("LEAD");
        assertThat(member.getPriority()).isEqualTo(5);
        assertThat(member.getOrgId()).isEqualTo("ORG1");
        assertThat(member.getOrgName()).isEqualTo("지점");
        OrgGroupMember memberDefault = OrgGroupMember.builder().groupCode("GRP1").orgId("ORG2").build();
        assertThat(memberDefault.getPriority()).isEqualTo(100);

        // no-args 생성자 커버리지
        OrgGroup emptyGroup = new OrgGroup();
        OrgGroupCategory emptyCat = new OrgGroupCategory();
        OrgGroupMember emptyMember = new OrgGroupMember();
        assertThat(emptyGroup).isNotNull();
        assertThat(emptyCat).isNotNull();
        assertThat(emptyMember).isNotNull();
    }
}
