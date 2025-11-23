package com.example.policy.datapolicy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrgGroupCoverageTest {

    @Test
    @DisplayName("OrgGroup/Category/Member builder 기본값과 필드 세터 검증")
    void orgGroupDefaults() {
        OrgGroup group = OrgGroup.builder()
                .code("SALES")
                .name("영업")
                .description("desc")
                .priority(10)
                .build();
        assertThat(group.getPriority()).isEqualTo(10);

        OrgGroupCategory category = OrgGroupCategory.builder()
                .code("SALES")
                .label("영업본부")
                .description("desc")
                .build();
        assertThat(category.getLabel()).isEqualTo("영업본부");

        OrgGroupCategoryMap map = OrgGroupCategoryMap.builder()
                .categoryCode("SALES")
                .groupCode("SALES-KR")
                .build();
        assertThat(map.getGroupCode()).isEqualTo("SALES-KR");

        OrgGroupMember member = OrgGroupMember.builder()
                .groupCode("SALES")
                .orgId("001")
                .orgName("강남지점")
                .leaderPermGroupCode("LEADER")
                .memberPermGroupCode("MEMBER")
                .priority(5)
                .build();
        assertThat(member.getPriority()).isEqualTo(5);
    }

    @Test
    @DisplayName("OrgGroupSettingsProperties는 YAML 기본값을 보존한다")
    void orgGroupSettingsPropertiesDefaults() {
        OrgGroupSettingsProperties props = new OrgGroupSettingsProperties();
        assertThat(props.getDefaultGroups()).isEmpty();
        assertThat(props.isFallbackToLowestPriorityGroup()).isTrue();
    }

    @Test
    @DisplayName("OrgGroupPermissionResolver는 리더/멤버 권한을 반환한다")
    void permissionResolver() {
        OrgGroupRepository repo = org.mockito.Mockito.mock(OrgGroupRepository.class);
        OrgGroupSettingsProperties props = new OrgGroupSettingsProperties();
        props.setDefaultGroups(java.util.List.of("DEFAULT-G"));
        OrgGroupPermissionResolver resolver = new OrgGroupPermissionResolver(repo, props);
        assertThat(resolver.resolvePermGroups(java.util.List.of("ORG1"), true)).contains("DEFAULT-G");
    }
}
